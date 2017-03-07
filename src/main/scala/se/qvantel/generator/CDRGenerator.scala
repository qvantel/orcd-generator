package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import se.qvantel.generator.model.{Call, Product}
import utils.Logger

object CDRGenerator extends App with SparkConnection with Logger {
  // Prepare batch
  val batchProduct = new BatchStatement()
  val batchCall = new BatchStatement()
  var count = 1
  val maxBatch = GenerateData.batchSize

  while (true) {
    // Generate random data
    batchCall.add(new SimpleStatement(Call.generateRecord()))
    batchProduct.add(new SimpleStatement(Product.generateRecord()))

    // Sleep for slowing down data transfer and more realistic timestamp intervall
    if(GenerateData.sleepTime() > 0) Thread.sleep(GenerateData.sleepTime())

    if (count == maxBatch) {
      session.execute(batchProduct)
      batchProduct.clear()
      session.execute(batchCall)
      batchCall.clear()
      count = 0
    }
    count = count + 1

  }

  // Close cassandra session
  session.close()

}
