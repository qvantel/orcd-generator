package model

object EventType extends Enumeration{
  type Service = Value
  val voice = Value("voice")
  val sms = Value("sms")
  val mms = Value("mms")
  val video = Value("video")
}
