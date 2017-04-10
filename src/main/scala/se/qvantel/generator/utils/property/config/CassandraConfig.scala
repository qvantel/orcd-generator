package se.qvantel.generator.utils.property.config

trait CassandraConfig extends Config {
  val ip = config.getString("cassandra.ip")
  val username = config.getString("cassandra.username")
  val password = config.getString("cassandra.password")
  val port = config.getString("cassandra.port")
  val keyspace = config.getString("cassandra.keyspace")
  val cdrTable = config.getString("cassandra.cdrTable")
}
