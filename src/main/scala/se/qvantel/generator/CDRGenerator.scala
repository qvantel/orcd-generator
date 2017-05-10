package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import com.typesafe.scalalogging.LazyLogging
import kamon.Kamon
import org.joda.time.{DateTime, DateTimeZone}
import scala.util.{Failure, Success, Try}
import model.EDR
import utils.property.config.{ApplicationConfig, CassandraConfig}
import utils.LastCdrChecker

object CDRGenerator extends App with SparkConnection
  with LazyLogging with CassandraConfig with ApplicationConfig {
  // Start Kamon
  Try(Kamon.start()) match {
    case Success(_) => logger.info("Kamon started sucessfully")
    case Failure(e) => {
      e.printStackTrace()
      System.exit(0)
    }
  }
  // Kamon metrics
  val cdrCounter = Kamon.metrics.counter("cdrs-generated")

  // Prepare batch
  val batch = new BatchStatement()
  var count = 1
  val maxBatchSize = GenerateData.batchSize
  var totalBatches = 0

  logger.info("Config: Nr of maximum batches: " + nrOfMaximumBatches)
  logger.info("Config: batch element size: " + maxBatchSize)

  val startTs = LastCdrChecker.getStartTime()
  logger.info(s"Start ts: $startTs")

  // An incrementing seed for every 1ns to avoid primary key collisions in cassandra
  var lastTsNs = 0L
  var seed = 0L

  var products = Products.readTrendsFromFile(startTs)
  logger.info(products.toString)

  // Save the day of the week. 1 represents Mondays, 2 Tuesdays and so on until 7 for Sundays.
  var nextDay = new DateTime(products.head._2 / 1000000, DateTimeZone.UTC).getDayOfWeek + 1
  if (nextDay == 8) {
    nextDay = (nextDay % 7)
  }

  while (totalBatches < nrOfMaximumBatches || nrOfMaximumBatches == -1) {
    val nextEntry = products.head
    val product = nextEntry._1
    val tsNs = nextEntry._2

    // Save the current day.
    val currentDay = new DateTime(tsNs / 1000000, DateTimeZone.UTC).getDayOfWeek

    // If new day, set nextDay to the next day. If next day is equal to 8 then set it to 1 (Monday). Change the trends.
    if (currentDay == nextDay) {
      nextDay = currentDay + 1
      if (nextDay == 8) {
        nextDay = (nextDay % 7)
      }
        products = Trends.randomizeTrends(products)
        logger.info(products.toString())
    }

    // Reset seed for next ns timestamp
    if (lastTsNs != tsNs) {
      lastTsNs = tsNs
      seed = -1
    }
    seed += 1
    if (seed > 999) {
      logger.error("More than 1000cdr/nanosecond, cassandra collisions will occur!!!")
    }

    // Sleep until next event to be generated
    val now = DateTime.now(DateTimeZone.UTC).getMillis
    val tsMs = tsNs / 1000000
    val sleeptime = tsMs - now
    if (sleeptime >= 0) {
      Thread.sleep(sleeptime)
    }
    // Generate and send CDR
    val execBatch = Try {
      // Generate CQL query for EDR
      val edrQuery = EDR.generateRecord(product, tsNs + seed)
      batch.add(new SimpleStatement(edrQuery))

      if (count == maxBatchSize) {
        session.execute(batch)
        batch.clear()
        count = 0
        totalBatches += 1
      }
      count += 1
      cdrCounter.increment()
    }

    if (execBatch.isSuccess) {
      // Calculate next time this type of event should be generated
      val nextTs = tsNs + Trends.nextTrendEventSleep(product, tsNs)
      products = products + (product -> (nextTs + seed))
    }
  }

    logger.info("Closing connection")
    // Close kamon and cassandra session
    session.close()
    Kamon.shutdown()
    logger.info("Closing program")
  }
