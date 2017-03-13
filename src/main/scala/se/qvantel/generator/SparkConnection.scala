package se.qvantel.generator

import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.{SparkConf, SparkContext}
import se.qvantel.generator.utils.Logger
import se.qvantel.generator.utils.property.CassandraConfig

trait SparkConnection extends CassandraConfig with Logger {
  // Configure spark->cassandra connection
  val conf = new SparkConf(true)
    .set("spark.cassandra.connection.host", ip)
    .set("spark.cassandra.auth.username", username)
    .set("spark.cassandra.auth.password", password)
    .set("spark.cassandra.connection.port", port)
  val context = new SparkContext("local[2]", "database", conf)
  // Setup cassandra connector
  val connector = CassandraConnector(conf)
  // Create cassandra session
  val session = connector.openSession()
}
