package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import org.joda.time.{DateTime, DateTimeZone}
import utils.Logger
import scala.util.{Failure, Success, Try}

import se.qvantel.generator.model.EDR
import se.qvantel.generator.model.product.Product
import se.qvantel.generator.utils.property.config.{ApplicationConfig, CassandraConfig}

object CDRGenerator extends App with SparkConnection
  with Logger with CassandraConfig with ApplicationConfig {
  // Prepare batch
  val batch = new BatchStatement()
  var count = 1
  val maxBatchSize = GenerateData.batchSize
  var totalBatches = 0

  logger.info("Config: Nr of maximum batches: " + nrOfMaximumBatches)
  logger.info("Config: batch element size: " + maxBatchSize)

  def getStartTime(): DateTime = {
    val backInTimeTs = DateTime.now(DateTimeZone.UTC).minusHours(backInTimeHours)
    // If sudden crash, look into the last inserted record and begin generating from that timestamp
    val rows = session.execute(
      s"SELECT created_at FROM $keyspace.$cdrTable " +
      "WHERE clustering_key=0 ORDER BY created_at DESC LIMIT 1")
      .all()

    // By default set startTs to backInTimeTs
    // If events exists in cassandra and last event is newer than backInTimeTs, start at lastEventTs
    // This is done in case for example the CDR Generator crashes or is shut down it will continue where it stopped
    val startTs = !rows.isEmpty match {
      case true => {
        val tsUs = rows.get(0).getLong("created_at")
        val lastEventTs = new DateTime(tsUs / 1000, DateTimeZone.UTC)
        logger.info(s"BackInTimeTs: $backInTimeTs")
        logger.info(s"LastEventTs: $lastEventTs")
        lastEventTs.getMillis > backInTimeTs.getMillis match {
          case true => lastEventTs
          case false => backInTimeTs
        }
      }
      case false => backInTimeTs
    }
    startTs
  }

  val startTs = getStartTime()
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
  // Close cassandra session
  session.close()
  logger.info("Closing program")
}
