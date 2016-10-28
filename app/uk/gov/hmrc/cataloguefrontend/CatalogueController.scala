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


import java.time.LocalDateTime

import play.api.Play.current
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.api.i18n.Messages.Implicits._
import play.api.mvc._
import play.api.{Configuration, Play}
import uk.gov.hmrc.cataloguefrontend.DisplayableTeamMembers.DisplayableTeamMember
import uk.gov.hmrc.cataloguefrontend.UserManagementConnector.{ConnectorError, TeamMember}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html._

object CatalogueController extends CatalogueController {

  override def userManagementConnector: UserManagementConnector = UserManagementConnector

  override def teamsAndServicesConnector: TeamsAndServicesConnector = TeamsAndServicesConnector

  override def indicatorsConnector: IndicatorsConnector = IndicatorsConnector

  override def releasesService: ReleasesService = new ReleasesService(ServiceReleasesConnector, TeamsAndServicesConnector)
}

trait CatalogueController extends FrontendController {

  def userManagementConnector: UserManagementConnector

  def teamsAndServicesConnector: TeamsAndServicesConnector

  def indicatorsConnector: IndicatorsConnector

  def releasesService: ReleasesService

  def landingPage() = Action { request =>
    Ok(landing_page())
  }

  def allTeams() = Action.async { implicit request =>
    teamsAndServicesConnector.allTeams.map { response =>
      Ok(teams_list(response.time, response.data.sortBy(_.toUpperCase)))
    }
  }

  def team(teamName: String) = Action.async { implicit request =>
    val teamMembersToggle: Option[String] = request.getQueryString("teamMembers")

    if(teamMembersToggle.isDefined) {

      for {
        typeRepos: Option[CachedItem[Map[String, Seq[String]]]] <- teamsAndServicesConnector.teamInfo(teamName)
        teamMembers: Either[ConnectorError, Seq[TeamMember]] <- userManagementConnector.getTeamMembers(teamName)
      } yield (typeRepos, teamMembers) match {
        case (Some(s), _) => Ok(team_info(s.time, teamName, repos = s.data, errorOrTeamMembers = convertToDisplayableTeamMembers(teamName, teamMembers)))

        //TODO: add extra error cases (or change the return type to be OK(team-info(?,Either[_, _] like above?)
        case (None, _) => NotFound
      }
    } else {
      for {
        typeRepos <- teamsAndServicesConnector.teamInfo(teamName)
      } yield typeRepos match {
        case Some(s) => Ok(team_info_without_members(s.time, teamName, repos = s.data, teamMembersLink = UserManagementPortalLink(teamName, Play.current.configuration)))
        case None => NotFound
      }
    }

  }

  def service(name: String) = Action.async { implicit request =>
    for {
      service <- teamsAndServicesConnector.repositoryDetails(name)
      maybeDataPoints <- indicatorsConnector.deploymentIndicatorsForService(name)
    } yield service match {
//      case Some(s) if s.data.repoType == RepoType.Deployable => Ok(service_info(s.time, s.data, indicators.map(_.map(_.toJSString))))
      case Some(s) if s.data.repoType == RepoType.Deployable => Ok(
        service_info(
        s.time, s.data,
        ChartData.deploymentThroughput(name, maybeDataPoints.map(_.throughput)),
        ChartData.deploymentStability(name, maybeDataPoints.map(_.stability)))
      )
      case _ => NotFound
    }
  }

  def library(name: String) = Action.async { implicit request =>
    for {
      library <- teamsAndServicesConnector.repositoryDetails(name)
    } yield library match {
      case Some(s) if s.data.repoType == RepoType.Library => Ok(library_info(s.time, s.data))
      case _ => NotFound
    }
  }

  def allServiceNames() = Action.async { implicit request =>
    teamsAndServicesConnector.allServiceNames.map { services =>
      Ok(services_list(services.time, repositories = services.data))
    }
  }

  def allLibraryNames() = Action.async { implicit request =>
    teamsAndServicesConnector.allLibraryNames.map { libraries =>
      Ok(library_list(libraries.time, repositories = libraries.data))
    }
  }

  def releases() = Action.async { implicit request =>

    import ReleaseFiltering._

    releasesService.getReleases().map { rs =>

      val form: Form[ReleasesFilter] = ReleasesFilter.form.bindFromRequest()
      form.fold(
        errors => Ok(release_list(Nil, errors)),
        query => Ok(release_list(rs.filter(query), form))
      )

    }

  }

  private def convertToDisplayableTeamMembers(teamName: String, errorOrTeamMembers: Either[ConnectorError, Seq[TeamMember]]) : Either[ConnectorError, Seq[DisplayableTeamMember]] =
    errorOrTeamMembers match {
      case Left(err) => Left(err)
      case Right(tms) => Right(DisplayableTeamMembers(teamName, tms))
    }



}

case class ReleasesFilter(team: Option[String] = None, serviceName: Option[String] = None, from: Option[LocalDateTime] = None, to: Option[LocalDateTime] = None) {
  def isEmpty: Boolean = team.isEmpty && serviceName.isEmpty && from.isEmpty && to.isEmpty
}

object ReleasesFilter {

  import DateHelper._

  lazy val form = Form(
    mapping(
      "team" -> optional(text).transform[Option[String]](x => if (x.exists(_.trim.isEmpty)) None else x, identity),
      "serviceName" -> optional(text).transform[Option[String]](x => if (x.exists(_.trim.isEmpty)) None else x, identity),
      "from" -> optionalLocalDateTimeMapping("from.error.date"),
      "to" -> optionalLocalDateTimeMapping("to.error.date")
    )(ReleasesFilter.apply)(ReleasesFilter.unapply)
  )

  def optionalLocalDateTimeMapping(errorCode: String): Mapping[Option[LocalDateTime]] = {
    optional(text.verifying(errorCode, x => stringToLocalDateTimeOpt(x).isDefined))
      .transform[Option[LocalDateTime]](_.flatMap(stringToLocalDateTimeOpt), _.map(_.format(`dd-MM-yyyy`)))
  }
}

object DisplayableTeamMembers {

  //!@ TODO read from config?
  val umpProfileBaseUrl = "http://example.com/profile/"

  def apply(teamName: String, teamMembers: Seq[TeamMember]): Seq[DisplayableTeamMember] = {

    val displayableTeamMembers = teamMembers.map(tm =>
      DisplayableTeamMember(
        displayName = tm.displayName.getOrElse("DISPLAY NAME NOT PROVIDED"),
        isServiceOwner = tm.serviceOwnerFor.map(_.map(_.toLowerCase)).exists(_.contains(teamName.toLowerCase)),
        umpLink = tm.username.map(umpProfileBaseUrl + _).getOrElse("USERNAME NOT PROVIDED")
      )
    )

    val (serviceOwners, others) = displayableTeamMembers.partition(_.isServiceOwner)
    serviceOwners.sortBy(_.displayName) ++ others.sortBy(_.displayName)
  }

  case class DisplayableTeamMember(displayName: String,
                                   isServiceOwner: Boolean = false,
                                   umpLink: String)

}

object UserManagementPortalLink {

  def apply(teamName: String, config: Configuration): String = {

    s"${config.getString("usermanagement.portal.url").fold("#")(x => s"$x/$teamName")}"

  }

}

