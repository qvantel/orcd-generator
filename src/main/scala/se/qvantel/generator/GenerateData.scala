package se.qvantel.generator

import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.model.{EventType, Service, TrafficCase, UnitOfMeasure}
import se.qvantel.generator.utils.property.config.{CallConfig, DataConfig}

import scala.util.Random

object GenerateData extends CallConfig with DataConfig {

  val mccList = getAvailableMccCodes()

  private def mcc(): String = {
    val int = Random.nextInt(mccList.length)
    this.mccList(int).toString
  }

  private def mnc(): String = "000"

  private def cell(): String = "FFFFFFFF"

  def destination(): String = mcc() + mnc() + cell()

  def msisdn(): String = {
    val randomStr = (1 to 10)
      .map(_ => Random.nextInt(10))
      .mkString

    randomStr
  }

  def getBackInTime(): Int = backInTimeHours

  def sleepTime(): Long = Math.abs(if(maxSleep>0)Random.nextLong()%maxSleep else 0)

  def amount(): Int = Random.nextInt(Integer.MAX_VALUE)%amountMax

  def timeStamp(): DateTime = DateTime.now(DateTimeZone.UTC)

  def timeStamp(days: Int): DateTime = DateTime.now(DateTimeZone.UTC).minusMinutes(days)

  def eventType(): String = EventType(Random.nextInt(EventType.maxId)).toString

  def trafficCase(): String = TrafficCase(Random.nextInt(TrafficCase.maxId)).toString

  def isRoaming(): Boolean = math.random < roamingChance

  def service(): String = Service(Random.nextInt(Service.maxId)).toString

  def unitOfMeasure(): String = UnitOfMeasure(Random.nextInt(UnitOfMeasure.maxId)).toString

  def product(): String = products(Random.nextInt(products.length))
}
