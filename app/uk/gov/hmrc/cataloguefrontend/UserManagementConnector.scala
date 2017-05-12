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

import cats.data.EitherT
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Reads}
import uk.gov.hmrc.cataloguefrontend.FutureHelpers.withTimerAndCounter
import uk.gov.hmrc.cataloguefrontend.config.WSHttp
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails

import scala.concurrent.Future


trait UserManagementConnector extends UserManagementPortalLink {
  val http: HttpGet

  import UserManagementConnector._

  implicit val teamMemberReads = Json.reads[TeamMember]
  implicit val teamDetailsReads = Json.reads[TeamDetails]

  implicit val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }



  def getTeamMembers(teams: Seq[String])(implicit hc: HeaderCarrier): Future[Either[ConnectorError, Map[String, Seq[TeamMember]]]] = {
    val x = Left(3)

//    x.

    type EitherTMap[A] = EitherT[Future, ConnectorError, A]

    val xs: Seq[EitherT[Future, ConnectorError, (String, Seq[TeamMember])]] = teams
      .map(teamName => EitherT(getTeamMembers(teamName)).map(x => (teamName, x)))

    val y: Seq[EitherT[Future, ConnectorError, Map[String, Seq[TeamMember]]]] =
      xs.map(x => x.map(Seq(_).toMap))


    PLATOPS-989
//    y.foldRight(Map.empty[String, Seq[TeamMember]])((a,b) => b ++ a.collectRight.value.get.get)

    import cats.syntax.applicative._

    import cats.Monoid

    val yy: EitherT[Future, ConnectorError, Map[String, Seq[TeamMember]]] =
      y.fold(Seq.empty[(String, Seq[TeamMember])].pure[EitherTMap])(Monoid[EitherT[Future, ConnectorError, Seq[(String, Seq[TeamMember])]]].combine)
        .map(_.toMap)

    yy.value

//    y.foldLeft(Map[String, Seq[TeamMember]].pure[EitherTMap])((acc, next: EitherT[Future, ConnectorError, Seq[TeamMember]]) => acc.combine())

  }
  def getTeamMembersNew(teams: Seq[String])(implicit hc: HeaderCarrier): Future[Either[ConnectorError, Map[String, Seq[TeamMember]]]] = {


    val xs: Future[Seq[(String, Either[ConnectorError, Seq[TeamMember]])]] = Future.sequence(teams
      .map(teamName => getTeamMembers(teamName).map((x: Either[ConnectorError, Seq[TeamMember]]) => (teamName, x))))


    val y: Future[Seq[Either[ConnectorError, Map[String, Seq[TeamMember]]]]] = xs.map{ s =>
      s.map {
        case (k, v) => v.right.map(s => (k,s)).right.map(Map(_))
      }
    }

    val blah: Future[Either[Seq[ConnectorError], Seq[Map[String, Seq[TeamMember]]]]] = y.map { (s: Seq[Either[ConnectorError, Map[String, Seq[TeamMember]]]]) =>
      s.partition(_.isLeft) match {
        case (Nil,  teamMaps) => Right(for(Right(i) <- teamMaps.view) yield i)
        case (connectionErrors, _) => Left(for(Left(s) <- connectionErrors.view) yield s)
      }
    }

    import cats.syntax.applicative._

    import cats.Monoid

//    val yy: EitherT[Future, ConnectorError, Map[String, Seq[TeamMember]]] =
//      y.fold(Seq.empty[(String, Seq[TeamMember])].pure[EitherTMap])(Monoid[EitherT[Future, ConnectorError, Seq[(String, Seq[TeamMember])]]].combine)
//        .map(_.toMap)
//
//    yy.value

//    y.foldLeft(Map[String, Seq[TeamMember]].pure[EitherTMap])((acc, next: EitherT[Future, ConnectorError, Seq[TeamMember]]) => acc.combine())

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

    val eventualConnectorErrorOrTeamMembers = http.GET[HttpResponse](url)(httpReads, newHeaderCarrier).map { response =>
      response.status match {
        case 200 => extractMembers(team, response)
        case httpCode => Left(HTTPError(httpCode))
      }
    }

    withTimerAndCounter("ump") (eventualConnectorErrorOrTeamMembers, isHttpCodeFailure)
      .recover {
      case ex =>
        Logger.error(s"An error occurred when connecting to $userManagementBaseUrl: ${ex.getMessage}", ex)
        Left(ConnectionError(ex))
    }
  }


  def getTeamDetails(team: String)(implicit hc: HeaderCarrier): Future[Either[ConnectorError, TeamDetails]] = {
    val newHeaderCarrier = hc.withExtraHeaders("requester" -> "None", "Token" -> "None")
    val url = s"$userManagementBaseUrl/v1/organisations/mdtp/teams/$team"
    withTimerAndCounter("ump-teamdetails") {
      http.GET[HttpResponse](url)(httpReads, newHeaderCarrier).map { response =>
        response.status match {
          case 200 => extractData[TeamDetails](team, response) {
            json =>
              (json \\ "organisations").lastOption.flatMap(x => (x \ "data").toOption)
          }
          case httpCode => Left(HTTPError(httpCode))
        }
      }.recover {
        case ex =>
          Logger.error(s"An error occurred when connecting to $userManagementBaseUrl: ${ex.getMessage}", ex)
          Left(ConnectionError(ex))
      }
    }

  }


  def extractData[T](team: String, response: HttpResponse)(extractor: JsValue => Option[JsValue])(implicit rd: Reads[T]): Either[ConnectorError, T] = {

   extractor(response.json).flatMap{ js => js.asOpt[T] } match {
     case Some(x) => Right(x)
     case _ => Left(NoData(umpMyTeamsPageUrl(team)))
   }
  }

  private def extractMembers(team: String, response: HttpResponse): Either[ConnectorError, Seq[TeamMember]] = {

    val left = Left(NoData(umpMyTeamsPageUrl(team)))

    val errorOrTeamMembers = (response.json \\ "members")
      .headOption
      .map(js => Right(js.as[Seq[TeamMember]]))
      .getOrElse(left)

    if (errorOrTeamMembers.isRight && errorOrTeamMembers.right.get.isEmpty) left else errorOrTeamMembers
  }


}

object UserManagementConnector extends UserManagementConnector {
  override val http = WSHttp

  sealed trait ConnectorError

  case class NoData(linkToRectify: String) extends ConnectorError

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

  case class TeamDetails(description: Option[String],
                         location: Option[String],
                         organisation: Option[String],
                         documentation: Option[String],
                         slack: Option[String],
                         team: String)


}
