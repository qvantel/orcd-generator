package se.qvantel.generator.model

import se.qvantel.generator.GenerateData

object Product extends EDR {
  // Duplicated in Call but keep for the moment as we might need to split up traffic cases
  val apname = ""

  // Insert random CDR data
  def generateRecord (): String = {
    // Insert product data
    val str = s"INSERT INTO qvantel.product (id, created_at, started_at, used_service_units, service, event_details, event_charges)" +
      s"VALUES (uuid(), '$timestamp', '$timestamp', " + // id, created_at and started_at
      s"{amount:$amount, unit_of_measure:'$unitOfMeasure', currency: '$currency'}, " + // used_service_units
      s"'$service'," + // service
      s"{access_point_name: '$apname', a_party_number: '$aPartyNumber', " + //event_details
      s"is_roaming: $isRoaming, a_party_location: {" + // event_details
      s"destination: '$apnDestination', location_number: '$apn_location_number', " + //a_party_location
      s"location_area_identification: '$apn_location_area_identification', " +
      s"cell_global_identification: '$apn_cell_global_identification'}}, " +
      s"{charged_units: {amount: $amount, unit_of_measure: '$unitOfMeasure', currency: '$currency'}," + //charged units
      s"product: {id: '$prid', name: '$prName'}," + //product
      s"charged_amounts: {{id: '$caid', name: '$caname', charged_type: '$cactype', event_type: '$caetype'," + //charged amounts
      s"resource_type: '$cartype', amount: $amount, end_balance: $endBalance, expiry_date: '$expiry_date'}}" +
      s"});" // end of cassandra statement
    str
  }

}
