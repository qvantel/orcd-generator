import java.time.LocalDateTime
import java.util.Random
import org.apache.spark._
import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector
import com.typesafe.scalalogging.Logger

object CDRGenerator {

  def main(args: Array[String]): Unit = {
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
    session.execute("CREATE TABLE IF NOT EXISTS database.cdr(key text PRIMARY KEY, value int, ts timestamp);")

    // Insert random CDR data
    val rand = new Random()
    while (true) {
      // Sleeps for random amount of time (0-2sec)
      Thread.sleep(Math.abs(rand.nextLong() % 2000))

      // Key label key0-key10
      val keyNum = Math.abs(rand.nextInt() % 11)
      val key = s"key$keyNum"
      val ts = LocalDateTime.now()
      val value = rand.nextInt()

      // Insert data
      session.execute(s"INSERT INTO database.cdr(key, value, ts) VALUES ('$key', $value, '$ts');")

      println(s"Inserted $key,$value,$ts")
    }

    // Close cassandra session
    session.close()
  }
}
