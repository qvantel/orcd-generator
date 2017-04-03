package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.model.EDR
import com.datastax.spark.connector._
import se.qvantel.generator.utils.property.config.ApplicationConfig
import utils.Logger
import scala.util.Random


object CDRGenerator extends App with SparkConnection with Logger with ApplicationConfig {
  // Prepare batch
  val batch = new BatchStatement()
  var count = 1
  val maxBatchSize = GenerateData.batchSize
  var totalBatches = 0

  logger.info("Config: Nr of maximum batches: " + nrOfMaximumBatches)
  logger.info("Config: batch element size: " + maxBatchSize)

  def getLastSync(): DateTime = {
    val cdrRdd = context.cassandraTable("qvantel", "cdr")
    val rows = cdrRdd.select("created_at")
      .where("clustering_key=0")
      .clusteringOrder(rdd.ClusteringOrder.Descending)
      .limit(1).collect()
    if (rows.length < 1) {
      new DateTime(0, DateTimeZone.UTC)
    }
    else {
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

  while (totalBatches < nrOfMaximumBatches || nrOfMaximumBatches == -1) {

    val nextEntry = products.head
    val product = nextEntry._1
    val tsMillis = nextEntry._2
    val tsNanos = tsMillis*1000 + (Random.nextInt()%1000)
    val ts = new DateTime(tsMillis, DateTimeZone.UTC)

    // Sleep until next event to be generated
    val sleeptime = tsMillis - DateTime.now(DateTimeZone.UTC).getMillis
    if (sleeptime >= 0) {
      Thread.sleep(sleeptime)
    }

    // Debug print
    val productname = product.name
    logger.info(s"$ts - $productname")

    // Generate CDR
    val edrQuery = EDR.generateRecord(product, tsNanos)
    batch.add(new SimpleStatement(edrQuery))

    // Calculate next time this type of event should be generated
    val nextTs = Trends.nextTrendEvent(product, tsMillis)
    products = products + (product -> nextTs)

    if (count == maxBatchSize) {
      session.execute(batch)
      batch.clear()
      count = 0
      //logger.info("Sent batch of " + maxBatchSize + " to Cassandra")
      totalBatches += 1
    }
    count += 1
  }

  logger.info("Closing connection")
  // Close cassandra session
  session.close()
  logger.info("Closing program")
}

