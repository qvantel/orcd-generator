package se.qvantel.generator

import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.utils.property.config.ProductConfig
import model.product.{CountryConfiguration, Product}
import se.qvantel.generator.utils.RandomUtils
import se.qvantel.generator.model.{Service, TrafficCase, UnitOfMeasure}

import scala.util.Random

object GenerateData extends ProductConfig {

  private val mccMap = getAvailableMccCodesByCountry
  private val isoMccMap = getIsoMccMap

  private def mcc(product : Product): String = {
    // Specify what country will get the mcc.

    // create key array of Tuple[iso, mcc]
    val keys: Map[String, String] = isoMccMap
    val iso = keys.map(i => i._1)


    // Create productList[CountryIsoName, modifier]
    val productCountries = product.countries
      .map(c => (c.country, c.modifier)).toMap

    // For every value in the iso list, check if modifier exists in product countries, else give defaultmodifier
    val maps = iso.map { i =>
      CountryConfiguration(i, product.defaultModifier)
    }.map { m =>
      if (productCountries.contains(m.country)) {
        m.copy(country = m.country, modifier = productCountries.get(m.country) match {
          case Some(mod) => mod
          case None => 0.0
        })
      }
      else{
        m
      }
    }

    // Remove elements with modifier < 0
    // create a List
    val b = maps
      .filter(_.modifier > 0)
      .map(a => (a.modifier, a.country)).toList

    // send List(modifier, iso) and get random
    val selectedIso = RandomUtils.weightedRandom(b)

    // covert iso to mcc and return, should never return 000
    keys.getOrElse(selectedIso,"000")
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

  def trafficCase(): String = TrafficCase(Random.nextInt(TrafficCase.maxId)).toString

  def isRoaming(): Boolean = math.random < roamingChance

  def service(): String = Service(Random.nextInt(Service.maxId)).toString

  def unitOfMeasure(): String = UnitOfMeasure(Random.nextInt(UnitOfMeasure.maxId)).toString
}
