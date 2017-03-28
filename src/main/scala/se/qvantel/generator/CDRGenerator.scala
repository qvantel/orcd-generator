package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import org.joda.time.DateTime
import se.qvantel.generator.model.EDR
import se.qvantel.generator.model.product.Product
import utils.Logger


object CDRGenerator extends App with SparkConnection with Logger {
  // Prepare batch
  val batch = new BatchStatement()
  var count = 1
  val maxBatchSize = GenerateData.batchSize
  val nrOfMaximumBatches = GenerateData.nrOfMaximumBatches
  var totalBatches = 0
  val cdrDensity = 15// How many cdr's per second since timestamp.
  val generateBackInTime = true// Disable to not generate back in time.

  def backInTimeGenerator(days : Int, hours : Int) : Unit = {
    var dayZ = 3
  }

  logger.info("Config: Nr of maximum batches: " + nrOfMaximumBatches)
  logger.info("Config: batch element size: " + maxBatchSize)

  //trend.setBackInTimeHours(GenerateData.backInTimeHours)
  var products = Trends.trends


  def nextTrendEvent(trend: Product, ts: Long) : Long = {
    val sleep = (1000/GenerateData.cdrModifier)/trend.points(0).cdrPerSec
    logger.info(sleep.toString)
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
    val product = nextEntry._1
    val tsMillis = nextEntry._2
    val ts = new DateTime(tsMillis)

    // Sleep until next event to be generated
    val sleeptime = tsMillis - DateTime.now().getMillis
    if (sleeptime >= 0) {
      Thread.sleep(sleeptime)
    }

    // Debug print
    val productname = product.name
    logger.info(s"$ts - $productname")

    // Generate CDR
    val edrQuery = EDR.generateRecord(product, ts)
    batch.add(new SimpleStatement(edrQuery))

    // Calculate next time this type of event should be generated
    val nextTs = nextTrendEvent(product, tsMillis)
    products = products + (product -> nextTs)

    if (count == maxBatchSize) {
      session.execute(batch)
      batch.clear()
      count = 0
      logger.info("Sent batch of " + maxBatchSize + " to Cassandra")
      totalBatches += 1
    }
    count += 1
  }

  logger.info("Closing connection")
  // Close cassandra session
  session.close()
  logger.info("Closing program")
}

