import java.util.Random

import utils.property.{CallConfig, DataConfig}

object GenerateData extends CallConfig with DataConfig{

  val mccList = getAvailableMccCodes()

  private def mcc(): String = {
    val rnd:Random = new Random()
    val int = rnd.nextInt(mccList.length - 0) + 0
    this.mccList(int).toString
  }

  private def mnc(): String = {
    "000"
  }

  private def cell(): String = {
    "FFFFFFFF"
  }

  def destination(): String = {
    // MCC + MNC + Whatever
    mcc() + mnc() + cell()
  }

  def msisdn(): String = {
    val rnd:Random = new Random()
    var ret = ""

    for(a<-1 to 10)
      ret=ret + (rnd.nextInt(9 - 0) + 0).toString

    ret
  }

  def isRoaming(): Boolean = {
    val PERCENTAGE_MAX = 100
    val rnd:Random = new Random()

    val dice = rnd.nextInt(PERCENTAGE_MAX-roamingChance)-roamingChance
    val k = if (dice>=roamingChance) true else false
    k
  }

  def product(): String = {
    val rnd:Random = new Random()

    val int = rnd.nextInt(products.length-0)-0
    this.products(int)
  }
}
