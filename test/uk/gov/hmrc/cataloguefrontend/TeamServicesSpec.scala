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

package uk.gov.hmrc.cataloguefrontend

import uk.gov.hmrc.cataloguefrontend.DateHelper._
import java.time.{ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter
import com.github.tomakehurst.wiremock.http.RequestMethod._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest._
import org.scalatestplus.play.OneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WS
import uk.gov.hmrc.cataloguefrontend.UserManagementConnector.TeamMember
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.cataloguefrontend.JsonData._
import scala.collection.JavaConversions._
import scala.io.Source

class TeamServicesSpec extends UnitSpec with BeforeAndAfter with OneServerPerSuite with WireMockEndpoints {

  def asDocument(html: String): Document = Jsoup.parse(html)

  val umpFrontPageUrl = "http://some.ump.fontpage.com"

  implicit override lazy val app = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.teams-and-services.host"      -> host,
      "microservice.services.teams-and-services.port"      -> endpointPort,
      "microservice.services.indicators.port"              -> endpointPort,
      "microservice.services.indicators.host"              -> host,
      "microservice.services.leak-detection.port"          -> endpointPort,
      "microservice.services.leak-detection.host"          -> host,
      "microservice.services.user-management.url"          -> endpointMockUrl,
      "usermanagement.portal.url"                          -> "http://usermanagement/link",
      "microservice.services.user-management.frontPageUrl" -> umpFrontPageUrl,
      "play.ws.ssl.loose.acceptAnyCertificate"             -> true,
      "play.http.requestHandler"                           -> "play.api.http.DefaultHttpRequestHandler"
    )
    .build()

  val teamName = "teamA"

  override def beforeEach(): Unit = {
    super.beforeEach()
    serviceEndpoint(GET, "/reports/repositories", willRespondWith = (200, Some("[]")))
  }

  "Team services page" should {

    "show a list of libraries, services, prototypes and repositories" in {
      serviceEndpoint(
        GET,
        "/api/teams_with_repositories",
        willRespondWith = (
          200,
          Some(
            """
             [
               {
                 "name": "teamA",
                 "firstActiveDate":1234560,
                 "lastActiveDate":1234561,
                 "repos":{
                    "Library": [
                        "teamA-lib"
                    ],
                    "Service": [
                        "teamA-serv",
                        "teamA-frontend"
                    ],
                    "Prototype": [
                        "service1-prototype",
                        "service2-prototype"
                    ],
                    "Other": [
                        "teamA-other"
                    ]
                 },
                 "ownedRepos": []
               }
             ]
            """
          ))
      )

      mockHttpApiCall(s"/v2/organisations/teams/$teamName/members", "/user-management-response.json")

      val response = await(WS.url(s"http://localhost:$port/teams/teamA").get)

      response.status shouldBe 200

      val htmlDocument = asDocument(response.body)
      val anchorTags   = htmlDocument.getElementsByTag("a").toList

      def assertAnchor(href: String, text: String): Unit =
        assert(anchorTags.exists { e =>
          e.text == text && e.attr("href") == href
        })

      assertAnchor("/library/teamA-lib", "teamA-lib")
      assertAnchor("/service/teamA-serv", "teamA-serv")
      assertAnchor("/service/teamA-frontend", "teamA-frontend")
      assertAnchor("/prototype/service1-prototype", "service1-prototype")
      assertAnchor("/prototype/service2-prototype", "service2-prototype")
      assertAnchor("/repositories/teamA-other", "teamA-other")

    }

    "show user management portal link" ignore {
      serviceEndpoint(
        GET,
        "/api/teams_with_repositories",
        willRespondWith = (
          200,
          Some(
            """
              [
                {
                "repos": { "Library": [], "Service": [] }, 
                "ownedRepos" = [] 
                }
              ]
            """
          ))
      )

      mockHttpApiCall(s"/v2/organisations/teams/$teamName/members", "/user-management-response.json")

      val response = await(WS.url(s"http://localhost:$port/teams/teamA").get)

      response.status shouldBe 200

      response.body.toString should include(
        """<a href="http://usermanagement/link/teamA" target="_blank">Team Members</a>""")

    }

    "show a message if no services are found" in {

      serviceEndpoint(
        GET,
        "/api/teams_with_repositories",
        willRespondWith = (
          200,
          Some(
            """
              [
                {
                  "name":"teamA",
                  "firstActiveDate":12345,
                  "lastActiveDate":1234567,
                  "repos": { "Library": [], "Service": [] },
                  "ownedRepos" : []
                }
              ]
            """
          ))
      )

      mockHttpApiCall(s"/v2/organisations/teams/$teamName/members", "/user-management-response.json")

      val response = await(WS.url(s"http://localhost:$port/teams/teamA").get)

      response.status shouldBe 200
      response.body   should include(ViewMessages.noRepoOfTypeForTeam("service"))
      response.body   should include(ViewMessages.noRepoOfTypeForTeam("library"))
    }

    "show team members correctly" in {
      val teamName = "CATO"

      serviceEndpoint(
        GET,
        "/api/teams_with_repositories",
        willRespondWith = (
          200,
          Some(
            """
              [
                {
                  "name":"CATO",
                  "firstActiveDate":12345,
                  "lastActiveDate":1234567,
                  "repos": { "Library": [], "Service": [] },
                  "ownedRepos": []
                }
              ]
            """
          ))
      )

      mockHttpApiCall(s"/v2/organisations/teams/$teamName/members", "/large-user-management-response.json")

      val response = await(WS.url(s"http://localhost:$port/teams/$teamName").get)

      response.status shouldBe 200
      val document = asDocument(response.body)

      verifyTeamMemberElementsText(document)

      verifyTeamMemberHrefLinks(document)

      verifyTeamOwnerIndicatorLabel(document)
    }

    "show a First Active and Last Active fields in the Details box" in {
      serviceEndpoint(
        GET,
        "/api/teams_with_repositories",
        willRespondWith = (
          200,
          Some(
            s"""
              [
               {
                "name": "teamA",
                "firstActiveDate":${createdAt.toInstant(ZoneOffset.UTC).toEpochMilli},
                "lastActiveDate":${lastActiveAt.toInstant(ZoneOffset.UTC).toEpochMilli},
                "repos":{
                   "Library": [
                   ],
                   "Service": [
                           "service1"
                   ]
                },
                "ownedRepos": []
               }
              ]
            """
          ))
      )

      mockHttpApiCall(s"/v2/organisations/teams/$teamName/members", "/user-management-response.json")
      val response = await(WS.url(s"http://localhost:$port/teams/$teamName").get)

      response.status shouldBe 200

      response.body should include(createdAt.displayFormat)
      response.body should include(lastActiveAt.displayFormat)
    }

    "show link to UMP's front page when no members node returned" ignore {

      def verifyForFile(fileName: String): Unit = {

        val teamName = "CATO"

        serviceEndpoint(
          GET,
          "/api/teams_with_repositories",
          willRespondWith = (
            200,
            Some(
              """
                [
                  {
                    "name": "teamA",
                    "firstActiveDate": 12345,
                    "lastActiveDate": 1234567,
                    "repos": {"Library":[], "Service": [] },
                    "ownedRepos": []
                  }
                ]
              """
            ))
        )

        mockHttpApiCall(s"/v2/organisations/teams/$teamName/members", fileName)

        val response = await(WS.url(s"http://localhost:$port/teams/$teamName").get)

        response.status shouldBe 200
        response.body   should include(umpFrontPageUrl)

        val document = asDocument(response.body)

        document
          .select("#linkToRectify")
          .text() shouldBe s"Team $teamName is not defined in the User Management Portal, please add it here"

      }

      verifyForFile("/user-management-empty-members.json")
      verifyForFile("/user-management-no-members.json")

    }

    "show error message if UMP is not available" in {
      serviceEndpoint(
        GET,
        "/api/teams_with_repositories",
        willRespondWith = (
          200,
          Some(
            """
              [
                {
                  "name": "teamA",
                  "firstActiveDate": 12345,
                  "lastActiveDate": 1234567,
                  "repos": {"Library":[], "Service": [] },
                  "ownedRepos" : []
                }
              ]
            """
          ))
      )

      mockHttpApiCall(
        url = s"/v2/organisations/teams/$teamName/members",
        "/user-management-response.json",
        httpCodeToBeReturned = 404)

      val response = await(WS.url(s"http://localhost:$port/teams/teamA").get)

      response.status shouldBe 200

      response.body should include("Sorry, the User Management Portal is not available")
    }

    "show team details correctly" in {
      val teamName = "CATO"

      serviceEndpoint(
        GET,
        "/api/teams_with_repositories",
        willRespondWith = (
          200,
          Some(
            s"""
              [
                {
                  "name":"$teamName",
                  "repos": { "Library": [], "Service": [] },
                  "ownedRepos": []
                }
              ]
            """
          ))
      )

      mockHttpApiCall(s"/v2/organisations/teams/$teamName", "/user-management-team-details-response.json")

      val response = await(WS.url(s"http://localhost:$port/teams/$teamName").get)

      response.status shouldBe 200
      val document = asDocument(response.body)

      val teamDetailsElements = document.select("#team_details li")
      teamDetailsElements.size() shouldBe 7

      teamDetailsElements(0).text() shouldBe "First Active: None"
      teamDetailsElements(1).text() shouldBe "Last Active: None"

      teamDetailsElements(2).text() shouldBe "Description: TEAM-A is a great team"
      teamDetailsElements(3).text() shouldBe "Documentation: Go to Confluence space"
      teamDetailsElements(3).toString() should include(
        """<a href="https://some.documentation.url" target="_blank">Go to Confluence space<span class="glyphicon glyphicon-new-window"""")

      teamDetailsElements(4).text() shouldBe "Organisation: ORGA"

      teamDetailsElements(5).text() shouldBe "Slack: Go to team channel"
      teamDetailsElements(5).toString() should include(
        """<a href="https://slack.host/messages/team-A" target="_blank">Go to team channel<span class="glyphicon glyphicon-new-window"""")

      teamDetailsElements(6).text() shouldBe "Location: STLPD"

    }

    "Render the frequent production indicators graph with throughput" in {
      serviceEndpoint(GET, "/api/teams_with_repositories", willRespondWith = (200, Some(teamsAndRepositories)))
      serviceEndpoint(
        GET,
        "/api/indicators/team/teamA/deployments",
        willRespondWith = (200, Some(deploymentThroughputData)))

      val response = await(WS.url(s"http://localhost:$port/teams/teamA").get)
      response.status shouldBe 200
      response.body   should include(s"""data.addColumn('string', 'Period');""")
      response.body   should include(s"""data.addColumn('number', 'Lead Time');""")
      response.body   should include(s"""data.addColumn('number', 'Interval');""")

      response.body should include(s"""data.addColumn({'type': 'string', 'role': 'tooltip', 'p': {'html': true}});""")
      response.body should include(s"""data.addColumn('number', 'Interval');""")
      response.body should include(s"""data.addColumn({'type': 'string', 'role': 'tooltip', 'p': {'html': true}});""")

      response.body should include(s"""chart.draw(data, options);""")

      response.body should include(s"""data.addColumn('string', 'Period');""")
      response.body should include(s"""data.addColumn('number', "Hotfix Rate");""")
      response.body should include(s"""data.addColumn({'type': 'string', 'role': 'tooltip', 'p': {'html': true}});""")
    }

  }

  "Render a message if the indicators service returns 404" in {
    serviceEndpoint(GET, "/api/teams_with_repositories", willRespondWith           = (200, Some(teamsAndRepositories)))
    serviceEndpoint(GET, "/api/indicators/team/teamA/deployments", willRespondWith = (404, None))

    val response = await(WS.url(s"http://localhost:$port/teams/teamA").get)

    response.status shouldBe 200
    response.body   should include(s"""No data to show""")
    response.body   should include(ViewMessages.noIndicatorsData)

    response.body shouldNot include(s"""chart.draw(data, options);""")
  }

  "Render a message if the indicators service encounters an error" in {
    serviceEndpoint(GET, "/api/teams_with_repositories", willRespondWith           = (200, Some(teamsAndRepositories)))
    serviceEndpoint(GET, "/api/indicators/team/teamA/deployments", willRespondWith = (500, None))

    val response = await(WS.url(s"http://localhost:$port/teams/teamA").get)
    response.status shouldBe 200
    response.body   should include(s"""The catalogue encountered an error""")
    response.body   should include(ViewMessages.indicatorsServiceError)

    response.body shouldNot include(s"""chart.draw(data, options);""")
  }

  def verifyTeamOwnerIndicatorLabel(document: Document): Unit = {
    val serviceOwnersLiLabels = document.select("#team_members li .label-success")
    serviceOwnersLiLabels.size()                         shouldBe 2
    serviceOwnersLiLabels.iterator().toSeq.map(_.text()) shouldBe Seq("Service Owner", "Service Owner")
  }

  def verifyTeamMemberHrefLinks(document: Document): Boolean = {
    val hrefs = document.select("#team_members [href]").iterator().toList

    hrefs.size shouldBe 5
    hrefs(0).attributes().get("href") == "http://example.com/profile/m.q"
    hrefs(1).attributes().get("href") == "http://example.com/profile/s.m"
    hrefs(2).attributes().get("href") == "http://example.com/profile/k.s"
    hrefs(3).attributes().get("href") == "http://example.com/profile/mx.p"
    hrefs(4).attributes().get("href") == "http://example.com/profile/ma.b"
  }

  def verifyTeamMemberElementsText(document: Document): Unit = {
    val teamMembersLiElements = document.select("#team_members li").iterator().toList

    teamMembersLiElements.length shouldBe 5

    teamMembersLiElements(0).text() should include("M Q Service Owner")
    teamMembersLiElements(1).text() should include("S M Service Owner")
    teamMembersLiElements(2).text() should include("K S")
    teamMembersLiElements(3).text() should include("Ma B")
    teamMembersLiElements(4).text() should include("Mx P")
  }

  def mockHttpApiCall(url: String, jsonResponseFile: String, httpCodeToBeReturned: Int = 200): String = {

    val json = readFile(jsonResponseFile)

    serviceEndpoint(method = GET, url = url, willRespondWith = (httpCodeToBeReturned, Some(json)))

    json
  }

  def readFile(jsonFilePath: String): String =
    Source.fromURL(getClass.getResource(jsonFilePath)).getLines().mkString("\n")

  def extractMembers(jsonString: String): Seq[TeamMember] =
    (Json.parse(jsonString) \\ "members").headOption
      .map(js => js.as[Seq[TeamMember]])
      .getOrElse(throw new RuntimeException(s"not able to extract team members from json: $jsonString"))

}
