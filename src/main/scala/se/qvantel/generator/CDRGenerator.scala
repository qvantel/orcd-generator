package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import se.qvantel.generator.model.{Call, Product}
import utils.Logger

object CDRGenerator extends App with SparkConnection with Logger {
  // Prepare batch
  val batchProduct = new BatchStatement()
  val batchCall = new BatchStatement()
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

  while (totalBatches < nrOfMaximumBatches || nrOfMaximumBatches == -1) {
    // Generate random data

    if(generateBackInTime){

    }

    else{
      batchCall.add(new SimpleStatement(Call.generateRecord()))
      batchProduct.add(new SimpleStatement(Product.generateRecord()))
      // Sleep for slowing down data transfer and more realistic timestamp intervall
      if(GenerateData.sleepTime() > 0) Thread.sleep(GenerateData.sleepTime())

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
  }

  logger.info("Closing connection")
  // Close cassandra session
  session.close()
  logger.info("Closing program")
}

