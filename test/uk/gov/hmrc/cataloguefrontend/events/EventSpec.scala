/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    it("should covert an Event with other event data type to json string") {
      val event = Event(EventType.Other, SomeOtherEventData("Catalogue", 1000l), 123l)

      val js = Json.toJson(event)

      Json.fromJson[Event](js) == event

      println(Json.prettyPrint(js))
    }

  }

}
