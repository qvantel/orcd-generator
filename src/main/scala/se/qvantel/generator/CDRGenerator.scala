package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import com.typesafe.scalalogging.Logger
import se.qvantel.generator.model.{Call, Product}

import scala.util.Random

object CDRGenerator extends App with SparkConnection {

  // Set up logging
  val logger = Logger("CDRGenerator")

  // Prepare batch
  val ps = session.prepare("INSERT INTO qvantel.call (id, created_at, started_at," +
    "used_service_units, service, event_details, event_charges) VALUES (?,?,?,?,?,?,?)")
  val batchCall = new BatchStatement()
  val batchProduct = new BatchStatement()
  var count = 1
  val maxBatch = GenerateData.batchSize

  while (true) {
    // Generate random data
    batchCall.add(new SimpleStatement(Call.generateBatch()))
    batchProduct.add(new SimpleStatement(Product.generateBatch()))

    // Sleep for slowing down data transfer and more realistic timestamp intervall
    Thread.sleep(Math.abs(Random.nextLong()%3))

    if (count == maxBatch) {
      session.execute(batchProduct)
      batchProduct.clear()
      logger.info("Product batch sent")
      session.execute(batchCall)
      batchCall.clear()
      logger.info("Call batch sent")
      count = 0
    }
    count = count + 1

  }

  // Close cassandra session
  session.close()

}
