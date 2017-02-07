import org.apache.spark._
import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector

object CDRGenerator {
  def main(args: Array[String]): Unit = {

    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", "127.0.0.1")
      .set("spark.cassandra.auth.username", "cassandra")
      .set("spark.cassandra.auth.password", "cassandra")
    val sc = new SparkContext("local[2]", "database", conf)

    val rdd = sc.cassandraTable("database", "cdr")
    CassandraConnector(conf).withSessionDo{ session =>
      session.execute("CREATE KEYSPACE IF NOT EXISTS database WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 };")
      session.execute("CREATE TABLE IF NOT EXISTS database.cdr(key text PRIMARY KEY, value int);")
      session.execute("INSERT INTO database.cdr(key, value) VALUES ('key1', 1);")
      session.execute("INSERT INTO database.cdr(key, value) VALUES ('key2', 2);")
    }
    //should print 2
    println(rdd.count())

    sc.stop()
  }
}


