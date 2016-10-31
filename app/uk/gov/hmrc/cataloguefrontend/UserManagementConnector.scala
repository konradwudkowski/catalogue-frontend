/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.cataloguefrontend

/*
 * Copyright 2016 HM Revenue & Customs
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

import java.time.LocalDateTime

import play.api.Logger
import uk.gov.hmrc.cataloguefrontend.config.WSHttp
import uk.gov.hmrc.play.config.ServicesConfig
import play.api.libs.json.Json
import uk.gov.hmrc.cataloguefrontend.FutureHelpers.withTimerAndCounter
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails

import scala.concurrent.Future



trait UserManagementConnector extends ServicesConfig {
  val http: HttpGet
  def userManagementBaseUrl: String

  import UserManagementConnector._

  implicit val teamMemberReads = Json.reads[TeamMember]

  implicit val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }

  def getTeamMembers(team: String)(implicit hc: HeaderCarrier): Future[Either[ConnectorError, Seq[TeamMember]]] = {
    val newHeaderCarrier = hc.withExtraHeaders("requester" -> "None", "Token" -> "None")

    val url = s"$userManagementBaseUrl/v1/organisations/mdtp/teams/$team/members"

    def isHttpCodeFailure: Option[(Either[ConnectorError, _]) => Boolean] =
      Some { errorOrMembers: Either[ConnectorError, _] =>
      errorOrMembers match {
        case Left(HTTPError(code)) if code != 200 => true
        case _ => false
      }
    }

    withTimerAndCounter("ump") {
      http.GET[HttpResponse](url)(httpReads, newHeaderCarrier).map { response =>
        response.status match {
          case 200 => extractMembers(team, response)
          case httpCode => Left(HTTPError(httpCode))
        }
      }
    }(isHttpCodeFailure).recover {
      case ex =>
        Logger.error(s"An error occurred when connecting to $userManagementBaseUrl: ${ex.getMessage}", ex)
        Left(ConnectionError(ex))
    }
  }

  private def extractMembers(team: String, response: HttpResponse): Either[ConnectorError, Seq[TeamMember]] = {
    val umpFrontPageUrl = getConfString(s"$serviceName.frontPageUrl", "#") + s"/$team"

    val left: Left[NoMembersField, Nothing] = Left(NoMembersField(umpFrontPageUrl))

    val errorOrTeamMembers: Either[NoMembersField, Seq[TeamMember]] with Product with Serializable = (response.json \\ "members")
      .headOption
      .map(js => Right(js.as[Seq[TeamMember]]))
      .getOrElse(left)

    if(errorOrTeamMembers.isRight && errorOrTeamMembers.right.get.isEmpty) left else errorOrTeamMembers
  }

}

object UserManagementConnector extends UserManagementConnector {
  override val http = WSHttp

  val serviceName = "user-management"

  override def userManagementBaseUrl: String =
    getConfString(s"$serviceName.url", throw new RuntimeException(s"Could not find config $serviceName.url"))

  sealed trait ConnectorError
  case class NoMembersField(linkToRectify:String) extends ConnectorError
  case class HTTPError(code: Int) extends ConnectorError
  case class ConnectionError(exception: Throwable) extends ConnectorError {
    override def toString: String = exception.getMessage
  }

  case class TeamMember(displayName: Option[String],
                         familyName: Option[String],
                         givenName: Option[String],
                         primaryEmail: Option[String],
                         serviceOwnerFor: Option[Seq[String]],
                         username: Option[String])
}

//GET : http://example.com//v1/organisations/mdtp/teams/DDCOps/members
//Headers :
//Token : None
//requester: None
