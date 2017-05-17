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

import java.sql.Timestamp
import java.time.ZoneOffset

import com.google.inject.{Inject, Singleton}
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.api.indexes.IndexType
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.mongo.{MongoConnector, ReactiveRepository}

import scala.concurrent.{ExecutionContext, Future}
import reactivemongo.api.indexes.Index
import uk.gov.hmrc.cataloguefrontend.FutureHelpers
import uk.gov.hmrc.cataloguefrontend.FutureHelpers.withTimerAndCounter
import uk.gov.hmrc.cataloguefrontend.events.EventType._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


trait EventRepository {
  def add(event: Event): Future[Boolean]
  def getEventsByType(eventType: EventType.Value): Future[Seq[Event]]
  def getAllEvents: Future[List[Event]]
  def clearAllData: Future[Boolean]
}

class MongoEventRepository(mongo: () => DB)
  extends ReactiveRepository[Event, BSONObjectID](
    collectionName = "events",
    mongo = mongo,
    domainFormat = Event.format) with EventRepository {


  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] =
    Future.sequence(
      Seq(
        collection.indexesManager.ensure(Index(Seq("eventType" -> IndexType.Hashed), name = Some("eventTypeIdx")))
      )
    )


//  def update(teamAndRepos: Event): Future[Event] = {
//
//    withTimerAndCounter("mongo.update") {
//      for {
//        update <- collection.update(selector = Json.obj("eventType" -> Json.toJson(teamAndRepos.teamName)), update = teamAndRepos, upsert = true)
//      } yield update match {
//        case lastError if lastError.inError => throw new RuntimeException(s"failed to persist $teamAndRepos")
//        case _ => teamAndRepos
//      }
//    }
//  }

  override def add(event: Event): Future[Boolean] = {
    withTimerAndCounter("mongo.write") {
      insert(event) map {
        case lastError if lastError.inError => throw lastError
        case _ => true
      }
    }
  }

//  def getForService(serviceName: String): Future[Option[Seq[Deployment]]] = {
//
//    withTimerAndCounter("mongo.read") {
//      find("name" -> BSONDocument("$eq" -> serviceName)) map {
//        case Nil => None
//        case data => Some(data.sortBy(_.productionDate.toEpochSecond(ZoneOffset.UTC)).reverse)
//      }
//    }
//  }

  override def getEventsByType(eventType: EventType.Value): Future[Seq[Event]] = {

    withTimerAndCounter("mongo.read") {
      find("eventType" -> BSONDocument("$eq" -> eventType.toString)) map {
        case Nil => Nil
        case data => data.sortBy(_.timestamp).reverse
      }
    }
  }


  override def getAllEvents: Future[List[Event]] = findAll()

  override def clearAllData: Future[Boolean] = super.removeAll().map(!_.hasErrors)

//  def deleteTeam(teamName: String): Future[String] = {
//    withTimerAndCounter("mongo.cleanup") {
//      collection.remove(query = Json.obj("teamName" -> Json.toJson(teamName))).map {
//        case lastError if lastError.inError => throw new RuntimeException(s"failed to remove $teamName")
//        case _ => teamName
//      }
//    }
//  }
}
