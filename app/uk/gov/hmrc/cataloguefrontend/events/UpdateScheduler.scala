/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Cancellable}
import play.Logger
import play.libs.Akka
import play.modules.reactivemongo.MongoDbConnection
import uk.gov.hmrc.cataloguefrontend.CatalogueController
import uk.gov.hmrc.cataloguefrontend.UserManagementConnector.TeamMember

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}
@Singleton
class UpdateScheduler @Inject()(actorSystem: ActorSystem, readModelService: ReadModelService) {

  def updateEventsReadModel: Future[Map[String, String]] = readModelService.refreshEventsCache
  def updateUmpCacheReadModel: Future[Seq[TeamMember]]   = readModelService.refreshUmpCache

  val initialDelay: FiniteDuration = 1 second

  def startUpdatingEventsReadModel(interval: FiniteDuration): Cancellable = {
    Logger.info(s"Initialising Event read model update every $interval")

    val scheduler = actorSystem.scheduler.schedule(initialDelay, interval) {
      updateEventsReadModel
    }

    scheduler
  }

  def startUpdatingUmpCacheReadModel(interval: FiniteDuration): Cancellable = {
    Logger.info(s"Initialising UMP cache read model update every $interval")

    val scheduler = actorSystem.scheduler.schedule(initialDelay, interval) {
      updateUmpCacheReadModel
    }

    scheduler
  }

}
