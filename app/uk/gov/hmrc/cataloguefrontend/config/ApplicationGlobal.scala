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

package uk.gov.hmrc.cataloguefrontend.config

import javax.inject.{Inject, Singleton}

import play.api._
import play.api.i18n.MessagesApi
import play.api.inject.ApplicationLifecycle
import play.api.mvc.Request
import uk.gov.hmrc.cataloguefrontend.events.UpdateScheduler
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


class CatalogueErrorHandler @Inject()(val messagesApi: MessagesApi, val configuration: Configuration) extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]) =
    views.html.error_template(pageTitle, heading, message)
}

@Singleton
class EventsReloadScheduler @Inject()(app: Application, override val configuration: Configuration) extends AppName {

  val eventReloadIntervalKey = "event.reload.interval"
  val umpCacheReloadIntervalKey = "ump.cache.reload.interval"

  Logger.info(s"Starting : $appName : in mode : ${app.mode}")
  Logger.debug("[Catalogue-frontend] - Starting... ")

  ApplicationCrypto.verifyConfiguration()

  scheduleEventsReloadSchedule(app)
  scheduleUmpCacheReloadSchedule(app)


  private def scheduleEventsReloadSchedule(app: Application) = {
    val maybeReloadInterval = app.configuration.getInt(eventReloadIntervalKey)

    maybeReloadInterval.fold {
      Logger.warn(s"$eventReloadIntervalKey is missing. Event cache reload will be disabled")
    } { reloadInterval =>
      Logger.warn(s"EventReloadInterval set to $reloadInterval seconds")
      val cancellable = UpdateScheduler.startUpdatingEventsReadModel(reloadInterval seconds)
      app.injector.instanceOf[ApplicationLifecycle].addStopHook(() => Future(cancellable.cancel()))
    }
  }

  private def scheduleUmpCacheReloadSchedule(app: Application) = {
    val maybeUmpCacheReloadInterval = app.configuration.getInt(umpCacheReloadIntervalKey)

    maybeUmpCacheReloadInterval.fold {
      Logger.warn(s"$umpCacheReloadIntervalKey is missing. Ump cache reload will be disabled")
    } { reloadInterval =>
      Logger.warn(s"UMP cache reload interval set to $reloadInterval seconds")
      val cancellable = UpdateScheduler.startUpdatingUmpCacheReadModel(reloadInterval seconds)
      app.injector.instanceOf[ApplicationLifecycle].addStopHook(() => Future(cancellable.cancel()))
    }
  }

}
