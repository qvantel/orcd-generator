package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import com.typesafe.scalalogging.LazyLogging
import kamon.Kamon
import org.joda.time.{DateTime, DateTimeZone}
import scala.util.{Failure, Success, Try}
import model.EDR
import model.product.Product
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
    val tsUs = nextEntry._2

    // Sleep until next event to be generated
    val sleeptime = (tsUs / 1000) - DateTime.now(DateTimeZone.UTC).getMillis
    if (sleeptime >= 0) {
      Thread.sleep(sleeptime)
    }
    // Generate and send CDR
    val execBatch = Try {
      // Generate CQL query for EDR
      val edrQuery = EDR.generateRecord(product, tsUs)
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

    execBatch match {
      case Success(_) => {
        // Calculate next time this type of event should be generated
        val nextTs = tsUs + Trends.nextTrendEventSleep(product, tsUs)
        products = products + (product -> nextTs)
      }
      case Failure(e) => {
        // Check if session is open, then close it and try to connect to Cassandra once again
        if (!session.isClosed) {
          session.close()
        }

        Try(session = connector.openSession()) match {
          case Success(_) => logger.info("Reconnected back to Cassandra")
          case Failure(e) => logger.info("Could not reconnect to cassandra, will attempt again.")
        }

      }
    }
  }

  logger.info("Closing connection")
  // Close kamon and cassandra session
  session.close()
  Kamon.shutdown()
  logger.info("Closing program")
}
