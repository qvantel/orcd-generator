package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import se.qvantel.generator.model.{Call, Product}
import utils.Logger

object CDRGenerator extends App with SparkConnection with Logger {
  // Prepare batch
  val batchProduct = new BatchStatement()
  val batchCall = new BatchStatement()
  var count = 1
  val batchSize = GenerateData.batchSize
  val nrOfMaximumBatches = GenerateData.nrOfMaximumBatches
  var totalBatches = 0

  logger.info("Config: Nr of maximum batches: " + nrOfMaximumBatches)
  logger.info("Config: batch element size: " + batchSize)

  while (totalBatches < nrOfMaximumBatches || nrOfMaximumBatches == -1)
  {
    // Generate random data
    batchCall.add(new SimpleStatement(Call.generateRecord()))
    batchProduct.add(new SimpleStatement(Product.generateRecord()))

    // Sleep for slowing down data transfer and more realistic timestamp intervall
    if(GenerateData.sleepTime() > 0) Thread.sleep(GenerateData.sleepTime())

    if (count == batchSize) {
      session.execute(batchProduct)
      batchProduct.clear()
      session.execute(batchCall)
      batchCall.clear()
      count = 0
      logger.info("Sent batch of " + batchSize + " to Cassandra")
      totalBatches = totalBatches + 1
    }
    count = count + 1

  }

  // Close cassandra session
  logger.info("Closing connection")
  session.close()

  logger.info("Closing program")

}
