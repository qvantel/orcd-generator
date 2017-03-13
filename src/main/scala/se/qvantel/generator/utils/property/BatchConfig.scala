package se.qvantel.generator.utils.property

trait BatchConfig extends Config {
  // Get the batch size to use as a limit
  val batchSize = config.getInt("gen.cassandra.element.batch.size")

  // Get the batch limit, which will be used as an exit criteria
  val nrOfMaximumBatches = config.getString("gen.batch.limit").toInt
}
