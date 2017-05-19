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


import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Cancellable}
import org.joda.time.Duration
import play.Logger
import play.inject.ApplicationLifecycle
import play.libs.Akka
import play.modules.reactivemongo.MongoDbConnection
import uk.gov.hmrc.cataloguefrontend.CatalogueController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._




trait DefaultSchedulerDependencies extends MongoDbConnection  {

  val akkaSystem = Akka.system()

}

abstract class Scheduler {
  def akkaSystem: ActorSystem

  def updateEventsReadModel: Unit = CatalogueController.readModelService.refreshEventsCache
  def updateUmpCacheReadModel: Unit = CatalogueController.readModelService.refreshUmpCache

  def startUpdating(interval: FiniteDuration): Cancellable = {
    Logger.info(s"Initialising Event read model update every $interval")

    val scheduler = akkaSystem.scheduler.schedule(100 milliseconds, interval) {
      updateEventsReadModel
    }

    scheduler
  }
  def startUpdatingUmp(interval: FiniteDuration): Cancellable = {
    Logger.info(s"Initialising UMP cache read model update every $interval")

    val scheduler = akkaSystem.scheduler.schedule(100 milliseconds, interval) {
      updateUmpCacheReadModel
    }

    scheduler
  }

}


object UpdateScheduler extends Scheduler with DefaultSchedulerDependencies

