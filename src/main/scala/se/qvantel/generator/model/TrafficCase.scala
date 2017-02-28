package se.qvantel.generator.model

object TrafficCase extends Enumeration{
  type Service = Value
  val originating = Value("originating")
  val terminating = Value("terminating")
  val forwarding = Value("forwarding")
}
