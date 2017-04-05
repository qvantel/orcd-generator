package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.model.EDR
import com.datastax.spark.connector._
import se.qvantel.generator.utils.property.config.{ApplicationConfig, CassandraConfig}
import utils.Logger
import scala.util.{Failure, Success, Try}


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

    // Debug print
    val productname = product.name
    logger.info(s"$ts - $productname")

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
  // Close cassandra session
  session.close()
  logger.info("Closing program")
}

