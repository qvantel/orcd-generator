package se.qvantel.generator

import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.model._
import se.qvantel.generator.utils.property.config.ProductConfig

import scala.util.Random

object GenerateData extends ProductConfig {

  private val mccMap = getAvailableMccCodesByCountry()
  private val isoMccMap = getIsoMccMap()

  private def mcc(product : Product): String = {
    // Specify what country will get the mcc.
    val keys = mccMap.keySet.toArray
    // val isoKeys: Array[String] = isoMccMap.keySet.toArray

    // isoMccMap
    // ["se" => "1", "it" => "2"]
    // isoKeys
    // ["se", "it"]

    // product.countryconfig.
    // List = list[CountryConfiguration]
    // list = [ { country: "se", mod: "2"}, {...} ]
    // val something: Array[String] = product.countryConfiguration.map(p => p.countryIsoName).toArray

    // Something
    // ["se", "...", ... ]

    // print(isoKeys.union(something))
    System.exit(1)


    val randomKey = keys(Random.nextInt(keys.length))
    val mccList = mccMap(randomKey)
    mccList(Random.nextInt(mccList.length)).toString
  }

  private def mnc(): String = "000"

  private def cell(): String = "FFFFFFFF"

  def destination(product : Product): String = mcc(product) + mnc() + cell()

  def msisdn(): String = {
    (1 to 10)
      .map(_ => Random.nextInt(10))
      .mkString
  }

  def amount(): Int = Random.nextInt(Integer.MAX_VALUE)%amountMax

  def timeStamp(): DateTime = DateTime.now(DateTimeZone.UTC)

  def timeStamp(days: Int): DateTime = DateTime.now(DateTimeZone.UTC).minusMinutes(days)

  def eventType(): String = EventType(Random.nextInt(EventType.maxId)).toString

  def trafficCase(): String = TrafficCase(Random.nextInt(TrafficCase.maxId)).toString

  def isRoaming(): Boolean = math.random < roamingChance

  def service(): String = Service(Random.nextInt(Service.maxId)).toString

  def unitOfMeasure(): String = UnitOfMeasure(Random.nextInt(UnitOfMeasure.maxId)).toString
}
