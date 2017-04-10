package se.qvantel.generator.model

import se.qvantel.generator.model.product.Product
import se.qvantel.generator.GenerateData

object EDR {
  def amount: Int = GenerateData.amount()
  def unitOfMeasure: String = GenerateData.unitOfMeasure()
  def isRoaming: Boolean = GenerateData.isRoaming()
  def aPartyNumber: String = GenerateData.msisdn()
  def apnDestination: String = GenerateData.destination()
  val clustering_key = 0
  var timestamp : Long = 0
  val apn_location_number = ""
  val apn_location_area_identification = ""
  val apn_cell_global_identification = ""
  val currency = ""
  val prid = ""
  var prName = ""
  val caid = ""
  val caname = ""
  var service  = ""
  val cactype = ""
  val caetype = ""
  val cartype = ""
  def endBalance: Int = GenerateData.amount()
  val expiry_date = ""

  def generateRecord(product: Product, tsNanos: Long): String = {
    service = product.serviceType
    prName = product.name
    timestamp = tsNanos
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

    val call = s"INSERT INTO qvantel.cdr (id, created_at, started_at, clustering_key, used_service_units, service, event_details, event_charges)" +
      s"VALUES (uuid(), $timestamp, $timestamp, $clustering_key, " + // id, created_at, started_at, clustering_key
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

    val prod = s"INSERT INTO qvantel.cdr (id, created_at, started_at, clustering_key, used_service_units, service, event_details, event_charges)" +
      s"VALUES (uuid(), $timestamp, $timestamp, $clustering_key, " + // id, created_at, started_at, clustering_key
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
