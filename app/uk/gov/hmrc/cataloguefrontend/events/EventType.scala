package uk.gov.hmrc.cataloguefrontend.events

import play.api.libs.json._

object EventType extends Enumeration {

  type EventType = Value

  val ServiceOwnerUpdated, Other = Value

  implicit val eventType = new Format[EventType] {
    override def reads(json: JsValue): JsResult[EventType] = json match {
      case JsString(s) => {
        try {
          JsSuccess(EventType.withName(s))
        } catch {
          case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${EventType.getClass}', but it does not appear to contain the value: '$s'")
        }
      }
      case _ => JsError("String value expected")
    }

    override def writes(o: EventType): JsValue = JsString(o.toString)
  }

}
