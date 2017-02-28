package se.qvantel.generator.model

import se.qvantel.generator.GenerateData

trait Call extends EDR {

    val traffic_case = TrafficCase(scala.util.Random.nextInt(TrafficCase.maxId))
    val event_type = EventType(scala.util.Random.nextInt(EventType.maxId))
    val b_party_number = GenerateData.msisdn()
    val bpn_destination = GenerateData.destination()
    val bpn_location_number = ""
    val bpn_location_area_identification = ""
    val bpn_cell_global_identification = ""

  def generateBatch(): String ={
    // Insert call data
    val str = s"INSERT INTO qvantel.call (id, created_at, started_at, used_service_units, service, event_details, event_charges)" +
      s"VALUES (uuid(), '$timestamp', '$timestamp', " + // id, created_at and started_at
      s"{amount:$amount, unit_of_measure:'$unit_of_measure', currency: '$currency'}, " + // used_service_units
      s"'$service'," + // service
      s"{traffic_case: '$traffic_case', event_type: '$event_type', a_party_number: '$a_party_number', " + //event_details
      s"b_party_number: '$b_party_number', is_roaming: $is_roaming, a_party_location: {" + // event_details
      s"destination: '$apn_destination', location_number: '$apn_location_number', " + //a_party_location
      s"location_area_identification: '$apn_location_area_identification', " +
      s"cell_global_identification: '$apn_cell_global_identification'}, " +
      s"b_party_location: {" + // b_party_location
      s"destination: '$bpn_destination', location_number: '$bpn_location_number', " +
      s"location_area_identification: '$bpn_location_area_identification', " +
      s"cell_global_identification: '$bpn_cell_global_identification'}}, " +
      s"{charged_units: {amount: $amount, unit_of_measure: '$unit_of_measure', currency: '$currency'}," + //charged units
      s"product: {id: '$prid', name: '$prname'}," + //product
      s"charged_amounts: {{id: '$caid', name: '$caname', charged_type: '$cactype', event_type: '$caetype'," + //charged amounts
      s"resource_type: '$cartype', amount: $amount, end_balance: $end_balance, expiry_date: '$expiry_date'}}" +
      s"});" // end of cassandra statement
    str
  }
}

object Call extends Call
