package se.qvantel.generator.model

import org.joda.time.{DateTime, DateTimeZone}
import se.qvantel.generator.GenerateData

import scala.util.Random

trait EDR {
  val timestamp = DateTime.now(DateTimeZone.UTC)
  val amount = Random.nextInt(Integer.MAX_VALUE)%1000
  val unit_of_measure = UnitOfMeasure(scala.util.Random.nextInt(UnitOfMeasure.maxId))
  val service = Service(scala.util.Random.nextInt(Service.maxId))
  val is_roaming = GenerateData.isRoaming()
  val a_party_number = GenerateData.msisdn()
  val apn_destination = GenerateData.destination()
  val apn_location_number = ""
  val apn_location_area_identification = ""
  val apn_cell_global_identification = ""
  val currency = ""
  val prid = ""
  val prname = GenerateData.product()
  val caid = ""
  val caname = ""
  val cactype = ""
  val caetype = ""
  val cartype = ""
  val end_balance = 0
  val expiry_date = ""
}
