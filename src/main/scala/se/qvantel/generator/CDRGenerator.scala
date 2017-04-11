package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import kamon.Kamon
import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.model.EDR
import se.qvantel.generator.utils.property.config.{ApplicationConfig, CassandraConfig}
import utils.{Logger, TimeManager}

import scala.util.{Failure, Success, Try}

object CDRGenerator extends App with SparkConnection
  with Logger with CassandraConfig with ApplicationConfig {
  // Start Kamon
  Try(Kamon.start()) match{
    case Success(_) => logger.info("Kamon started sucessfully")
    case Failure(e) => e.printStackTrace()
      System.exit(0)
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

  val startTs = TimeManager.getStartTime()
  logger.info(s"Start ts: $startTs")

  var products = Trends.readTrendsFromFile(startTs)
  logger.info(products.toString)

  while (totalBatches < nrOfMaximumBatches || nrOfMaximumBatches == -1) {

    val nextEntry = products.head
    val product = nextEntry._1
    val ts = new DateTime(nextEntry._2, DateTimeZone.UTC)

    // Sleep until next event to be generated
    val sleeptime = ts.getMillis - DateTime.now(DateTimeZone.UTC).getMillis
    if (sleeptime >= 0) {
      Thread.sleep(sleeptime)
    }
    // Generate and send CDR
    val execBatch = Try {
      // Convert epoch timestamp from milli seconds to micro seconds
      val tsNanos = ts.getMillis*1000 + (System.nanoTime()%1000)
      // Generate CQL query for EDR
      val edrQuery = EDR.generateRecord(product, tsNanos)
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
        val nextTs = ts.getMillis + Trends.nextTrendEventSleep(product, ts)
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

