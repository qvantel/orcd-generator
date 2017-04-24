package se.qvantel.generator.model

import se.qvantel.generator.model.product.Product
import se.qvantel.generator.GenerateData

case class ServiceUnitModel
(
  amount: Int,
  currency: String,
  unitOfMeasure: String
) {
  def toCqlValues(): String = {
    s"{amount: $amount, currency: '$currency', unit_of_measure: '$unitOfMeasure'}"
  }
}

case class LocationInformationModel
(
  destination: String,
  locationNumber: String,
  locationAreaIdentification: String,
  cellGlobalIdentification: String
) {
  def toCqlValues(): String = {
    "{" +
      s"destination: '$destination', " +
      s"location_number: '$locationNumber', " +
      s"location_area_identification: '$locationAreaIdentification', " +
      s"cell_global_identification: '$cellGlobalIdentification'"+
    "}"
  }
}

case class EventDetailsModel
(
  accessPointName: String,
  trafficCase: String,
  eventType: String,
  isRoaming: Boolean,
  aPartyNumber: String,
  bPartyNumber: String,
  aPartyLocation: LocationInformationModel,
  bPartyLocation: LocationInformationModel
) {
  def toCqlValues(): String = {
    val apl = aPartyLocation.toCqlValues()
    var bpl = "null"
    if (bPartyLocation != null){
      bpl = bPartyLocation.toCqlValues()
    }
    s"{" +
      s"access_point_name: '$accessPointName', " +
      s"traffic_case: '$trafficCase', " +
      s"event_type: '$eventType', " +
      s"is_roaming: $isRoaming, " +
      s"a_party_number: '$aPartyNumber', " +
      s"b_party_number: '$bPartyNumber', " +
      s"a_party_location: $apl, " +
      s"b_party_location: $bpl" +
    "}"
  }
}

case class ChargedAmountsModel
(
  id: String,
  name: String,
  chargedType: String,
  eventType: String,
  resourceType: String,
  amount: Int,
  endBalance: Int,
  expiryDate: String
) {
  def toCqlValues(): String = {
    "{" +
      s"id: '$id', " +
      s"name: '$name', " +
      s"charged_type: '$chargedType', " +
      s"event_type: '$eventType', " +
      s"resource_type: '$resourceType', " +
      s"amount: $amount, " +
      s"end_balance: $endBalance, " +
      s"expiry_date: '$expiryDate'" +
    "}"
  }
}

case class ProductModel
(
  id: String,
  name: String
) {
  def toCqlValues(): String = {
    "{" +
      s"id: '$id', " +
      s"name: '$name'" +
    "}"
  }
}

case class EventChargesModel
(
  chargedUnits: ServiceUnitModel,
  product: ProductModel,
  chargedAmounts: ChargedAmountsModel
)
{
  def toCqlValues(): String = {
    val cu = chargedUnits.toCqlValues()
    val p = product.toCqlValues()
    val ca = chargedAmounts.toCqlValues()
    s"{" +
      s"charged_units: $cu, " +
      s"product: $p, " +
      s"charged_amounts: {$ca}" +
    "}"
  }
}

case class EDRModel
(
  createdAt: Long,
  startedAt: Long,
  service: String,
  usedServiceUnits: ServiceUnitModel,
  eventDetails: EventDetailsModel,
  eventCharges: EventChargesModel,
  clusteringKey: Int
) {
  def toCqlValues(): String = {
    val usu = usedServiceUnits.toCqlValues()
    val ed = eventDetails.toCqlValues()
    val ec = eventCharges.toCqlValues()
    s"uuid(), $createdAt, $startedAt, '$service', $usu, $ed, $ec, $clusteringKey"
  }
}

object EDR {
  private def generateLocationInformation(): LocationInformationModel ={
    LocationInformationModel(
      destination = GenerateData.destination(),
      locationNumber = "",
      locationAreaIdentification = "",
      cellGlobalIdentification = ""
    )
  }

  def generateRecord(product: Product, tsNanos: Long): String = {
    var eventDetails = EventDetailsModel(
      accessPointName = GenerateData.destination(),
      trafficCase = "",
      eventType = "",
      isRoaming = GenerateData.isRoaming(),
      aPartyNumber = GenerateData.msisdn(),
      bPartyNumber = null,
      aPartyLocation = generateLocationInformation(),
      bPartyLocation = null
    )
    if (product.serviceType == "voice"){
      eventDetails = eventDetails.copy(
        eventDetails.accessPointName,
        eventDetails.trafficCase,
        eventDetails.eventType,
        eventDetails.isRoaming,
        eventDetails.aPartyNumber,
        eventDetails.bPartyNumber,
        eventDetails.aPartyLocation,
        generateLocationInformation()
      )
    }
    val usedServiceUnits = ServiceUnitModel(
      amount = GenerateData.amount(),
      currency = "",
      unitOfMeasure = GenerateData.unitOfMeasure()
    )
    val chargedServiceUnits = ServiceUnitModel(
      amount = GenerateData.amount(),
      currency = "",
      unitOfMeasure = GenerateData.unitOfMeasure()
    )
    val chargedAmounts = ChargedAmountsModel(
      id = "",
      name = "",
      chargedType = "",
      eventType = "",
      resourceType = "",
      amount = usedServiceUnits.amount,
      endBalance = GenerateData.amount(),
      expiryDate = ""
    )
    val prod = ProductModel(
      id = product.campaignId,
      name = product.name
    )
    val eventCharges = EventChargesModel(
      chargedUnits = chargedServiceUnits,
      product = prod,
      chargedAmounts = chargedAmounts
    )
    val edr = EDRModel(
      createdAt = tsNanos,
      startedAt = tsNanos,
      service = product.serviceType,
      usedServiceUnits = usedServiceUnits,
      eventDetails = eventDetails,
      eventCharges = eventCharges,
      clusteringKey = 0
    )
    generateRecord(edr)
  }

  private def generateRecord(edr: EDRModel): String = {
    "INSERT INTO qvantel.cdr (id, created_at, started_at, service, used_service_units, event_details, event_charges, clustering_key) " +
    "VALUES (" + edr.toCqlValues() + ");"
  }
}
