package se.qvantel.generator.model

import se.qvantel.generator.GenerateData

trait Product extends EDR {
  val traffic_case = TrafficCase(scala.util.Random.nextInt(TrafficCase.maxId))
  val event_type = EventType(scala.util.Random.nextInt(EventType.maxId))
  val apname = ""
  val ucell_global_identification = ""

  // Insert random CDR data
  def generateBatch(): String = {
    // Insert product data
    val str = s"INSERT INTO qvantel.product (id, created_at, started_at, used_service_units, service, event_details, event_charges)" +
      s"VALUES (uuid(), '$timestamp', '$timestamp', " + // id, created_at and started_at
      s"{amount:$amount, unit_of_measure:'$unit_of_measure', currency: '$currency'}, " + // used_service_units
      s"'$service'," + // service
      s"{access_point_name: '$apname', a_party_number: '$a_party_number', " + //event_details
      s"is_roaming: $is_roaming, a_party_location: {" + // event_details
      s"destination: '$apn_destination', location_number: '$apn_location_number', " + //a_party_location
      s"location_area_identification: '$apn_location_area_identification', " +
      s"cell_global_identification: '$ucell_global_identification'}}, " +
      s"{charged_units: {amount: $amount, unit_of_measure: '$unit_of_measure', currency: '$currency'}," + //charged units
      s"product: {id: '$prid', name: '$prname'}," + //product
      s"charged_amounts: {{id: '$caid', name: '$caname', charged_type: '$cactype', event_type: '$caetype'," + //charged amounts
      s"resource_type: '$cartype', amount: $amount, end_balance: $end_balance, expiry_date: '$expiry_date'}}" +
      s"});" // end of cassandra statement
    str
  }

}

object Product extends Product
