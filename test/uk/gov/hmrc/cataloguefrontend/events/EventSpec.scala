package uk.gov.hmrc.cataloguefrontend.events

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, FunSuite, Matchers}
import play.api.libs.json.Json

class EventSpec extends FunSpec with Matchers with MockitoSugar {

  describe("Json write") {

    it("should convert a json string to an Event with ServiceOwnerUpdatedEventData") {
      val jsonString = """{
        |  "eventType" : "ServiceOwnerUpdated",
        |  "eventData" : {
        |    "type" : "ServiceOwnerUpdatedEventData",
        |    "data" : {
        |      "service" : "Catalogue",
        |      "name" : "Armin Keyvanloo"
        |    }
        |  },
        |  "timestamp" : 123
        |}""".stripMargin

      Json.parse(jsonString).as[Event] == Event(EventType.ServiceOwnerUpdated, ServiceOwnerUpdatedEventData("Catalogue", "Armin Keyvanloo"), 123l)
    }

    it("should covert an Event with ServiceOwnerUpdatedEventData to json string") {
      val event = Event(EventType.ServiceOwnerUpdated, ServiceOwnerUpdatedEventData("Catalogue", "Armin Keyvanloo"), 123l)

      val js = Json.toJson(event)

      Json.fromJson[Event](js) == event

    }

  }

}
