package se.qvantel.generator.utils.property.config

trait ApplicationConfig extends Config {

  // If the application should use threads
  val threaded = config.getString("gen.threaded").equals("yes")

  // Get the batch size to use as a limit
  val batchSize = config.getInt("gen.cassandra.element.batch.size")

  // The limit of amount to generate(0-max)
  val amountMax = config.getInt("gen.cassandra.element.amount.max")

  // Number of batchSizes until the program is ready to exit. -1 for infinity
  val nrOfMaximumBatches = config.getInt("gen.batch.limit")

  // Scale up or scale down the amount of cdrs generated (1 for realistic values)
  val cdrModifier = config.getDouble("gen.modifier")

  // Total amount of hours to generate back in time
  val backInTimeHours = config.getInt("gen.backintime")
}
