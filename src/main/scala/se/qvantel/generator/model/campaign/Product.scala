package se.qvantel.generator.model.campaign

case class Product(name: String, serviceType: String, campaignId: String, startHour : String, endHour : String, points : List[Point])
