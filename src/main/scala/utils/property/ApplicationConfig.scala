package utils.property

trait ApplicationConfig extends Config {
  // If the application should use threads
  val threaded = if (config.getString("gen.threaded").equals("yes")) true else false

  // Get an interval between NOW and x seconds ago
  val maximumSecondLimit = config.getObject("gen.timestamp.maximum.seconds.behind").toString.toInt

  // Get the batch size to use as a limit
  val batchSize = config.getObject("gen.cassandra.batch.size").toString.toInt

}
