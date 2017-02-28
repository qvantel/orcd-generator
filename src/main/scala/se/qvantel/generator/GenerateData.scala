package se.qvantel.generator


import se.qvantel.generator.utils.property.{CallConfig, DataConfig}
import utils.property.CallConfig

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
    val ten = 10
    val randomStr = (1 to 10)
      .map(_ => Random.nextInt(ten))
      .mkString

    randomStr
  }

  def isRoaming(): Boolean = math.random < roamingChance

  def product(): String = {
    val int = Random.nextInt(products.length-0)-0
    this.products(int)
  }
}
