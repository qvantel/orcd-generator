import com.datastax.driver.core.{BatchStatement, SimpleStatement}
import org.apache.spark._
import com.datastax.spark.connector.cql.CassandraConnector
import com.typesafe.scalalogging.Logger
import model.{EventType, Service, TrafficCase, UnitOfMeasure}
import org.joda.time.{DateTime, DateTimeZone}

import scala.util.Random

object CDRGenerator extends App {

  // Set up logging
  val logger = Logger("CDRGenerator")

  // Configure spark->cassandra connection
  val conf = new SparkConf(true)
    .set("spark.cassandra.connection.host", "127.0.0.1")
    .set("spark.cassandra.auth.username", "cassandra")
    .set("spark.cassandra.auth.password", "cassandra")
  val context = new SparkContext("local[2]", "database", conf)
  // Setup cassandra connector
  val connector = CassandraConnector(conf)
  // Create cassandra session
  val session = connector.openSession()

  // Prepare batch
  val ps = session.prepare("INSERT INTO qvantel.call (id, created_at, started_at," +
    "used_service_units, service, event_details, event_charges) VALUES (?,?,?,?,?,?,?)")
  val batchCall = new BatchStatement()
  val batchProduct = new BatchStatement()
  var count = 1
  val maxBatch = GenerateData.batchSize

  // Insert random CDR data
  def generateProductBatch(): String ={
    val timestamp = DateTime.now(DateTimeZone.UTC)
    val value = Random.nextInt(Integer.MAX_VALUE)%1000
    val unit_of_measure = UnitOfMeasure(scala.util.Random.nextInt(UnitOfMeasure.maxId))
    val service = Service(scala.util.Random.nextInt(Service.maxId))
    val traffic_case = TrafficCase(scala.util.Random.nextInt(TrafficCase.maxId))
    val event_type = EventType(scala.util.Random.nextInt(EventType.maxId))
    val is_roaming = math.random < 0.25 // 25% chance of user roaming = true
    val currency = ""
    val prid = ""
    val prname = GenerateData.product()
    val caid = ""
    val caname = ""
    val cactype = ""
    val caetype = ""
    val cartype = ""
    val eb = 0
    val ed = ""
    val apname = ""
    val udestination = ""
    val ulocation_number = ""
    val ulocation_area_identification = ""
    val rint = 999
    val a_party_number = Iterator.continually("447700900" + f"${Random.nextInt(rint)}%03d")

    val ucell_global_identification = ""
    // Insert product data
    val str = s"INSERT INTO qvantel.product (id, created_at, started_at, used_service_units, service, event_details, event_charges)" +
      s"VALUES (uuid(), '$timestamp', '$timestamp', " + // id, created_at and started_at
      s"{amount:$value, unit_of_measure:'$unit_of_measure', currency: '$currency'}, " + // used_service_units
      s"'$service'," + // service
      s"{access_point_name: '$apname', a_party_number: '$a_party_number', " + //event_details
      s"is_roaming: $is_roaming, a_party_location: {" + // event_details
      s"destination: '$udestination', location_number: '$ulocation_number', " + //a_party_location
      s"location_area_identification: '$ulocation_area_identification', " +
      s"cell_global_identification: '$ucell_global_identification'}}, " +
      s"{charged_units: {amount: $value, unit_of_measure: '$unit_of_measure', currency: '$currency'}," + //charged units
      s"product: {id: '$prid', name: '$prname'}," + //product
      s"charged_amounts: {{id: '$caid', name: '$caname', charged_type: '$cactype', event_type: '$caetype'," + //charged amounts
      s"resource_type: '$cartype', amount: $value, end_balance: $eb, expiry_date: '$ed'}}" +
      s"});" // end of cassandra statement
    str
  }

  def generateCallBatch(): String ={
    val timestamp = DateTime.now(DateTimeZone.UTC)
    val value = Random.nextInt(Integer.MAX_VALUE)%1000
    val unit_of_measure = UnitOfMeasure(scala.util.Random.nextInt(UnitOfMeasure.maxId))
    val service = Service(scala.util.Random.nextInt(Service.maxId))
    val traffic_case = TrafficCase(scala.util.Random.nextInt(TrafficCase.maxId))
    val event_type = EventType(scala.util.Random.nextInt(EventType.maxId))
    val is_roaming = GenerateData.isRoaming()
    val a_party_number = GenerateData.msisdn()
    val b_party_number = GenerateData.msisdn()
    val apn_destination = GenerateData.destination()
    val apn_location_number = ""
    val apn_location_area_identification = ""
    val apn_cell_global_identification = ""
    val bpn_destination = GenerateData.destination()
    val bpn_location_number = ""
    val bpn_location_area_identification = ""
    val bpn_cell_global_identification = ""
    val currency = ""
    val prid = ""
    val prname = GenerateData.product()
    val caid = ""
    val caname = ""
    val cactype = ""
    val caetype = ""
    val cartype = ""
    val eb = 0
    val ed = ""

    // Insert call data
    val str = s"INSERT INTO qvantel.call (id, created_at, started_at, used_service_units, service, event_details, event_charges)" +
      s"VALUES (uuid(), '$timestamp', '$timestamp', " + // id, created_at and started_at
      s"{amount:$value, unit_of_measure:'$unit_of_measure', currency: '$currency'}, " + // used_service_units
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
      s"{charged_units: {amount: $value, unit_of_measure: '$unit_of_measure', currency: '$currency'}," + //charged units
      s"product: {id: '$prid', name: '$prname'}," + //product
      s"charged_amounts: {{id: '$caid', name: '$caname', charged_type: '$cactype', event_type: '$caetype'," + //charged amounts
      s"resource_type: '$cartype', amount: $value, end_balance: $eb, expiry_date: '$ed'}}" +
      s"});" // end of cassandra statement
    str
  }

  while(true) {
    // Generate random data
    batchCall.add(new SimpleStatement(generateProductBatch()))
    batchProduct.add(new SimpleStatement(generateCallBatch()))

    // Sleep for slowing down data transfer and more realistic timestamp intervall
    Thread.sleep(Math.abs(Random.nextLong()%3))

    if(count==maxBatch){
      session.execute(batchProduct)
      batchProduct.clear()
      logger.info("Product batch sent")
      session.execute(batchCall)
      batchCall.clear()
      logger.info("Call batch sent")
      count = 0
    }
    count = count + 1

  }

  // Close cassandra session
  session.close()

}
