package utils.property

trait ApplicationConfig extends Config {
  // If the application should use threads
  val threaded = if (config.getString("gen.threaded").equals("yes")) true else false

  // Get the batch size to use as a limit
  val batchSize = config.getInt("gen.cassandra.element.batch.size")

}
