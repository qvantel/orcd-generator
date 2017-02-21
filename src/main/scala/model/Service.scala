package model

object Service extends Enumeration {
  type Service = Value
  val voice = Value("voice")
  val sms = Value("sms")
  val mms = Value("mms")
  val data = Value("data")
}
