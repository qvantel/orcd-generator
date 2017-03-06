package se.qvantel.generator.utils.property

/**
  *  Data(products) specific configuration
  */
trait DataConfig extends ApplicationConfig {
  val products = config.getString("gen.products")
    .split(";")
}
