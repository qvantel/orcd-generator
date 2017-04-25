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

  var products = Products.readTrendsFromFile(startTs)
  logger.info(products.toString)

  while (totalBatches < nrOfMaximumBatches || nrOfMaximumBatches == -1) {

    val nextEntry = products.head
    val product = nextEntry._1
    val tsNs = nextEntry._2

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
      val edrQuery = EDR.generateRecord(product, tsNs)
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
      products = products + (product -> (nextTs + System.nanoTime()%1000))
    }
  }

  logger.info("Closing connection")
  // Close kamon and cassandra session
  session.close()
  Kamon.shutdown()
  logger.info("Closing program")
}
