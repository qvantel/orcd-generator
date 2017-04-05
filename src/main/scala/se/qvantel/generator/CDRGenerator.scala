package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.model.EDR
import com.datastax.spark.connector._
import se.qvantel.generator.utils.property.config.{ApplicationConfig, CassandraConfig}
import utils.Logger

import scala.util.{Failure, Random, Success, Try}


object CDRGenerator extends App with SparkConnection
  with Logger with CassandraConfig with ApplicationConfig {
  // Prepare batch
  val batch = new BatchStatement()
  var count = 1
  val maxBatchSize = GenerateData.batchSize
  var totalBatches = 0

  logger.info("Config: Nr of maximum batches: " + nrOfMaximumBatches)
  logger.info("Config: batch element size: " + maxBatchSize)

  def getLastSync(): DateTime = {
    val cdrRdd = context.cassandraTable(keyspace, cdrTable)
    // If sudden crash, look into the last inserted record and begin generating from that timestamp
    val rows = cdrRdd.select("created_at")
      .where("clustering_key=0")
      .clusteringOrder(rdd.ClusteringOrder.Descending)
      .limit(1)
      .collect()

    // If there is no records in the cassandra database, the time to start the generator is now.
    if (rows.length < 1) {
      new DateTime(0, DateTimeZone.UTC)
    }
    else {
      // Otherwise, get the latest record and generate from that timestamp to now.
      val lastSyncNs = rows.apply(0).getLong(0)
      new DateTime(lastSyncNs / 1000, DateTimeZone.UTC)
    }
  }

  val lastSync = getLastSync()
  var startTs = DateTime.now(DateTimeZone.UTC).minusHours(backInTimeHours)
  logger.info(s"Last sync ts: $lastSync")
  logger.info(s"Back in time ts: $startTs")
  if (lastSync.getMillis > startTs.getMillis){
    startTs = lastSync
  }
  logger.info(s"Start ts: $startTs")

  var products = Trends.readTrendsFromFile(startTs)
  logger.info(products.toString)

  while (totalBatches < nrOfMaximumBatches || nrOfMaximumBatches == -1) {

    val nextEntry = products.head
    val product = nextEntry._1
    val tsMillis = nextEntry._2
    // Convert epoch timestamp from milli seconds to nano seconds
    val tsNanos = tsMillis*1000 + (Random.nextInt()%1000)
    val ts = new DateTime(tsMillis, DateTimeZone.UTC)

    val execBatch = Try {
      // Sleep until next event to be generated
      val sleeptime = tsMillis - DateTime.now(DateTimeZone.UTC).getMillis
      logger.info(products.toString)
      if (sleeptime >= 0) {
        Thread.sleep(sleeptime)
      }

      // Debug print
      val productname = product.name
      logger.info(s"$ts - $productname")

      // Generate CDR
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
        val nextTs = tsMillis + Trends.nextTrendEventSleep(product, ts)
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

