package se.qvantel.generator.utils.property.config

import com.typesafe.config.ConfigFactory

trait Config {
  val config = ConfigFactory.load()
}
