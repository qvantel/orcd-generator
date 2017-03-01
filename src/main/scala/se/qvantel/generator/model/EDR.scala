package se.qvantel.generator.model

import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.GenerateData

import scala.util.Random

trait EDR {
  def timestamp: DateTime = DateTime.now(DateTimeZone.UTC)
  def amount: Int = Random.nextInt(Integer.MAX_VALUE)%1000
  def unitOfMeasure: String = UnitOfMeasure(scala.util.Random.nextInt(UnitOfMeasure.maxId)).toString
  def service: String = Service(scala.util.Random.nextInt(Service.maxId)).toString
  def isRoaming: Boolean = GenerateData.isRoaming()
  def aPartyNumber: String = GenerateData.msisdn()
  def apnDestination: String = GenerateData.destination()
  val apn_location_number = ""
  val apn_location_area_identification = ""
  val apn_cell_global_identification = ""
  val currency = ""
  val prid = ""
  def prName: String = GenerateData.product()
  val caid = ""
  val caname = ""
  val cactype = ""
  val caetype = ""
  val cartype = ""
  val end_balance = 0
  val expiry_date = ""
}
