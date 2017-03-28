package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import org.joda.time.DateTime
import se.qvantel.generator.model.campaign.Product
import utils.Logger


object CDRGenerator extends App with SparkConnection with Logger {
  // Prepare batch
  val batchProduct = new BatchStatement()
  val batchCall = new BatchStatement()
  var count = 1
  val maxBatchSize = GenerateData.batchSize
  val nrOfMaximumBatches = GenerateData.nrOfMaximumBatches
  var totalBatches = 0

  logger.info("Config: Nr of maximum batches: " + nrOfMaximumBatches)
  logger.info("Config: batch element size: " + maxBatchSize)

  var products = Trends.trends

  def nextTrendEvent(trend: Product, ts: Long) : Long = {
    val sleep = (1000/GenerateData.cdrModifier)/trend.points(0).cdrPerSec
    println(sleep)
    val next = ts + sleep
    next.toLong
    /*
    val now = DateTime.now().getMillis
    if (trend.startHour != null && trend.endHour != null) {
      val start = now + DateTime.parse(trend.startHour).getSecondOfDay * 1000
      val end = now + DateTime.parse(trend.endHour).getSecondOfDay * 1000
      if (now > start && now < end) {

      }
    }
    */
  }
  while (totalBatches < nrOfMaximumBatches || nrOfMaximumBatches == -1) {
    val nextEntry = products.head

    // Sleep until next event to be generated
    val sleeptime = nextEntry._2 - DateTime.now().getMillis
    if (sleeptime >= 0)
      Thread.sleep(sleeptime)

    // Debug print
    val ts = new DateTime(nextEntry._2)
    val productname = nextEntry._1.name
    println(s"$ts - $productname")

    // Generate CDR
    nextEntry._1.serviceType match {
      case "voice" => print("voice")
      case "data" => print("data")
    }

    // Calculate next time this type of event should be generated
    val nextTs = nextTrendEvent(nextEntry._1, nextEntry._2)
    products = products + (nextEntry._1 -> nextTs)

    if (count == maxBatchSize) {
      session.execute(batchProduct)
      batchProduct.clear()
      session.execute(batchCall)
      batchCall.clear()
      count = 0
      logger.info("Sent batch of " + maxBatchSize + " to Cassandra")
      totalBatches = totalBatches + 1
    }
    count = count + 1
  }

  logger.info("Closing connection")
  // Close cassandra session
  session.close()
  logger.info("Closing program")
}
