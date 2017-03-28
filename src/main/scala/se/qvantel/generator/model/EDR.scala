package se.qvantel.generator.model

import org.joda.time.DateTime
import se.qvantel.generator.GenerateData

object EDR {
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
  def endBalance: Int = GenerateData.amount()
  val expiry_date = ""

  def generateRecord(): String = {
    service match {
      case "voice" => generateVoiceRecord()
      case _ => generateDataRecord()
    }
  }

  private def generateVoiceRecord(): String = {
    // Call specific generation variables
    def trafficCase: String = GenerateData.trafficCase()
    def eventType: String = GenerateData.eventType()
    def bPartyNumber: String = GenerateData.msisdn()
    def bpnDestination: String = GenerateData.destination()
    // Move to method if it is non static
    val bpn_location_number = ""
    val bpn_location_area_identification = ""
    val bpn_cell_global_identification = ""

    val call = s"INSERT INTO qvantel.cdr (id, created_at, started_at, used_service_units, service, event_details, event_charges)" +
      s"VALUES (uuid(), '$timestamp', '$timestamp', " + // id, created_at and started_at
      s"{amount:$amount, unit_of_measure:'$unitOfMeasure', currency: '$currency'}, " + // used_service_units
      s"'$service'," + // service
      s"{traffic_case: '$trafficCase', event_type: '$eventType', a_party_number: '$aPartyNumber', " + //event_details
      s"b_party_number: '$bPartyNumber', is_roaming: $isRoaming, a_party_location: {" + // event_details
      s"destination: '$apnDestination', location_number: '$apn_location_number', " + //a_party_location
      s"location_area_identification: '$apn_location_area_identification', " +
      s"cell_global_identification: '$apn_cell_global_identification'}, " +
      s"b_party_location: {" + // b_party_location
      s"destination: '$bpnDestination', location_number: '$bpn_location_number', " +
      s"location_area_identification: '$bpn_location_area_identification', " +
      s"cell_global_identification: '$bpn_cell_global_identification'}}, " +
      s"{charged_units: {amount: $amount, unit_of_measure: '$unitOfMeasure', currency: '$currency'}," + //charged units
      s"product: {id: '$prid', name: '$prName'}," + //product
      s"charged_amounts: {{id: '$caid', name: '$caname', charged_type: '$cactype', event_type: '$caetype'," + //charged amounts
      s"resource_type: '$cartype', amount: $amount, end_balance: $endBalance, expiry_date: '$expiry_date'}}" +
      s"});" // end of cassandra statement
    call
  }

  private def generateDataRecord(): String = {
    // Product specific generation variables
    val apname = ""

    val prod = s"INSERT INTO qvantel.cdr (id, created_at, started_at, used_service_units, service, event_details, event_charges)" +
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
    prod
  }
}
