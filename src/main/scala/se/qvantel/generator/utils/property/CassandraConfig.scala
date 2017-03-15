package se.qvantel.generator.utils.property


trait CassandraConfig extends Config {
  val ip = config.getString("cassandra.ip")
  val username = config.getString("cassandra.username")
  val password = config.getString("cassandra.password")
}
