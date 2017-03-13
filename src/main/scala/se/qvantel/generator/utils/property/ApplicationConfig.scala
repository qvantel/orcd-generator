package se.qvantel.generator.utils.property

trait ApplicationConfig extends Config {

  // If the application should use threads
  val threaded = config.getString("gen.threaded").equals("yes")

  // The limit of amount to generate(0-max)
  val amountMax = config.getInt("gen.cassandra.element.amount.max")

  // The max amount of time the thread can sleep
  val maxSleep = config.getInt("gen.thread.max.sleeptime")
}
