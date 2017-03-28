package se.qvantel.generator.utils.property.config

/**
  *  Data(products) specific configuration
  */
trait DataConfig extends ApplicationConfig {
  val products = config.getString("gen.products")
    .split(";")
}
