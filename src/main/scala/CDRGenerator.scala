import java.time.LocalDateTime
import java.util.Random

import com.datastax.driver.core.{BatchStatement, BoundStatement, PreparedStatement, SimpleStatement}
import org.apache.spark._
import com.datastax.spark.connector.cql.CassandraConnector
import com.typesafe.scalalogging.Logger
import org.joda.time.{DateTime, DateTimeZone}

object CDRGenerator extends App{

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

  // Setup database
  session.execute("CREATE KEYSPACE IF NOT EXISTS database WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 };")
  // Drop table (In case table columns change, when CDR spec is fixed we can remove this)
  session.execute("DROP TABLE IF EXISTS database.cdr;")
  // Create table
  session.execute("CREATE TABLE IF NOT EXISTS database.cdr(key uuid PRIMARY KEY, value int, ts timestamp)")

  // Prepare batch
  val ps = session.prepare("INSERT INTO database.cdr (key, value, ts) VALUES (?,?,?)")
  val batch = new BatchStatement()
  var count = 1
  // Insert random CDR data
  val rand = new Random()
  while(true) {
    val ts = DateTime.now(DateTimeZone.UTC)
    val value = rand.nextInt()%10
    Thread.sleep(Math.abs(rand.nextLong() % 3))

    // Insert data
    batch.add(new SimpleStatement(s"INSERT INTO database.cdr (key, value, ts) VALUES (uuid(), $value, '$ts')"))
    if(count==1000){
      session.execute(batch)
      batch.clear()
      logger.info("Batch sent")
      count = 0
    }
    count = count + 1
  }

  // Close cassandra session
  session.close()

}
