package se.qvantel.generator.model

object UnitOfMeasure extends Enumeration {
  type UnitOfMeasure = Value
  val seconds = Value("seconds")
  val bytes = Value("bytes")
  val monetary = Value("monetary")
  val serviceSpecific = Value("service-specific")
}

