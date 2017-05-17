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

import play.api.libs.json._


sealed trait EventData
case class ServiceOwnerUpdatedEventData(service: String, name: String) extends EventData
case class SomeOtherEventData(something: String, somethingElse: Long) extends EventData // <--- this is an example event type showing how to add other EventData types


object EventData extends EventData {
  implicit val  serviceOwnerUpdatedEventDataFormat = Json.format[ServiceOwnerUpdatedEventData]
  implicit val  someOtherEventDataFormat = Json.format[SomeOtherEventData]

  def unapply(eventData: EventData): Option[(String, JsValue)] = {
    val (prod: Product, sub) = eventData match {
      case eventData: ServiceOwnerUpdatedEventData => (eventData, Json.toJson(eventData)(serviceOwnerUpdatedEventDataFormat))
      case eventData: SomeOtherEventData => (eventData, Json.toJson(eventData)(someOtherEventDataFormat))
    }
    Some(prod.productPrefix -> sub)
  }

  def apply(`type`: String, data: JsValue): EventData = {
    (`type` match {
      case "ServiceOwnerUpdatedEventData" => Json.fromJson[ServiceOwnerUpdatedEventData](data)(serviceOwnerUpdatedEventDataFormat)
      case "SomeOtherEventData" => Json.fromJson[SomeOtherEventData](data)(someOtherEventDataFormat)
    }).get
  }

  implicit val eventDataFmt = Json.format[EventData]
}

case class Event(eventType: EventType.Value, eventData: EventData, timestamp: Long)
object Event {
  implicit val format = Json.format[Event]
}








