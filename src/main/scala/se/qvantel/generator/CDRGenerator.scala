package se.qvantel.generator

import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import com.datastax.spark.connector.cql.CassandraConnector
import com.typesafe.scalalogging.Logger
import org.apache.spark._
import model.Product
import model.Call

import scala.util.Random

object CDRGenerator extends App {

  // Set up logging
  val logger = Logger("CDRGenerator")

  // Configure spark->cassandra connection
  val conf = new SparkConf(true)
    .set("spark.cassandra.connection.host", "127.0.0.1")
    .set("spark.cassandra.auth.username", "cassandra")
    .set("spark.cassandra.auth.password", "cassandra")
  val context = new SparkContext("local[2]", "database", conf)
  // Setup cassandra connector
  val connector = CassandraConnector(conf)
  // Create cassandra session
  val session = connector.openSession()

  // Prepare batch
  val ps = session.prepare("INSERT INTO qvantel.call (id, created_at, started_at," +
    "used_service_units, service, event_details, event_charges) VALUES (?,?,?,?,?,?,?)")
  val batchCall = new BatchStatement()
  val batchProduct = new BatchStatement()
  var count = 1
  val maxBatch = GenerateData.batchSize

  while(true) {
    // Generate random data
    batchCall.add(new SimpleStatement(Call.generateBatch()))
    batchProduct.add(new SimpleStatement(Product.generateBatch()))

    // Sleep for slowing down data transfer and more realistic timestamp intervall
    Thread.sleep(Math.abs(Random.nextLong()%3))

    if (count==maxBatch) {
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
