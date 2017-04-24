package se.qvantel.generator

import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.model.{TrafficCase, UnitOfMeasure}
import se.qvantel.generator.utils.property.config.CallConfig

import scala.util.Random

object GenerateData extends CallConfig {

  private val mccMap = getAvailableMccCodesByCountry()

  private def mcc(): String = {
    val keys = mccMap.keySet.toArray
    val randomKey = keys(Random.nextInt(keys.length))
    val mccList = mccMap(randomKey)
    mccList(Random.nextInt(mccList.length)).toString
  }

  private def mnc(): String = "000"

  private def cell(): String = "FFFFFFFF"

  def destination(): String = mcc() + mnc() + cell()

  def msisdn(): String = {
    (1 to 10)
      .map(_ => Random.nextInt(10))
      .mkString
  }

  def amount(): Int = Random.nextInt(Integer.MAX_VALUE)%amountMax

  def timeStamp(): DateTime = DateTime.now(DateTimeZone.UTC)

  def timeStamp(days: Int): DateTime = DateTime.now(DateTimeZone.UTC).minusMinutes(days)

  def trafficCase(): String = TrafficCase(Random.nextInt(TrafficCase.maxId)).toString

  def isRoaming(): Boolean = math.random < roamingChance

  def unitOfMeasure(): String = UnitOfMeasure(Random.nextInt(UnitOfMeasure.maxId)).toString
}
