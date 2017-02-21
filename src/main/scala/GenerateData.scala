
import utils.property.{CallConfig, DataConfig}
import scala.util.Random

object GenerateData extends CallConfig with DataConfig {

  val mccList = getAvailableMccCodes()

  private def mcc(): String = {
    val int = Random.nextInt(mccList.length - 0) + 0
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
    val randomStr = (1 to 10)
      .map(_ => Random.nextInt(10))
      .mkString

    randomStr
  }

  def isRoaming(): Boolean = {
    val PERCENTAGE_MAX = 100

    val dice = Random.nextInt(PERCENTAGE_MAX-roamingChance)-roamingChance
    val k = if (dice>=roamingChance) true else false
    k
  }

  def product(): String = {
    val int = Random.nextInt(products.length-0)-0
    this.products(int)
  }
}
