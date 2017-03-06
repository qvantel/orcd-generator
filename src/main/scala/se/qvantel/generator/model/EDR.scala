package se.qvantel.generator.model

import org.joda.time.DateTime
import se.qvantel.generator.GenerateData

trait EDR {
  def timestamp: DateTime = GenerateData.timeStamp()
  def amount: Int = GenerateData.amount()
  def unitOfMeasure: String = GenerateData.unitOfMeasure()
  def service: String = GenerateData.service()
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
