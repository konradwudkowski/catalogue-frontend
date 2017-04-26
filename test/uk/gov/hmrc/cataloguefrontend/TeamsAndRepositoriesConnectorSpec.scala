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

import com.github.tomakehurst.wiremock.http.RequestMethod._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{BeforeAndAfter, Matchers, OptionValues, WordSpec}
import org.scalatestplus.play.OneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeHeaders
import uk.gov.hmrc.play.http.HeaderCarrier

class TeamsAndRepositoriesConnectorSpec extends WordSpec with Matchers with BeforeAndAfter with ScalaFutures with OneServerPerSuite with WireMockEndpoints with TypeCheckedTripleEquals with OptionValues {

  import uk.gov.hmrc.cataloguefrontend.JsonData._
  implicit val defaultPatienceConfig = PatienceConfig(Span(200, Millis), Span(15, Millis))

  implicit override lazy val app = new GuiceApplicationBuilder().configure(
    "microservice.services.teams-and-services.host" -> host,
    "microservice.services.teams-and-services.port" -> endpointPort,
    "play.http.requestHandler" -> "play.api.http.DefaultHttpRequestHandler"
  ).build()


  "teamsByService" should {

    "return a list of team information for each given service" in {

      serviceEndpoint(POST, "/api/services?teamDetails=true", willRespondWith = (200, Some(
        """
          |	{
          |		"serviceA": ["teamA","teamB"],
          |		"serviceB": ["teamA"]
          |	}
          | """.stripMargin
      )), givenJsonBody = Some("[\"serviceA\",\"serviceB\"]"))

      val response = TeamsAndRepositoriesConnector.teamsByService(Seq("serviceA", "serviceB"))(HeaderCarrier.fromHeadersAndSession(FakeHeaders())).futureValue

      response.data.size shouldBe 2
      response.data("serviceA") shouldBe Seq("teamA", "teamB")
      response.data("serviceB") shouldBe Seq("teamA")

    }
  }

  "repositoryDetails" should {
    "convert the json string to RepositoryDetails" in {
      serviceEndpoint(GET, "/api/repositories/service-1", willRespondWith = (200, Some(JsonData.serviceDetailsData)))

      val responseData: RepositoryDetails =
        TeamsAndRepositoriesConnector
          .repositoryDetails("service-1")(HeaderCarrier.fromHeadersAndSession(FakeHeaders()))
          .futureValue
          .value
          .data

      responseData.name shouldBe "service-1"
      responseData.description shouldBe "some description"
      responseData.createdAt shouldBe createdAt
      responseData.lastActive shouldBe lastActiveAt
      responseData.teamNames should ===(Seq("teamA", "teamB"))
      responseData.githubUrls should ===(Seq(Link("github", "github.com", "https://github.com/hmrc/service-1")))
      responseData.ci should ===(Seq(Link("open1", "open 1", "http://open1/service-1"), Link("open2", "open 2", "http://open2/service-2")))
      responseData.environments should ===(Some(Seq(
        Environment(
          "Dev",
          Seq(
            Link("jenkins", "Jenkins", "https://deploy-dev.tax.service.gov.uk/job/deploy-microservice"),
            Link("grafana", "Grafana", "https://grafana-dev.tax.service.gov.uk/#/dashboard")
          )),
                Environment(
          "QA",
          Seq(
            Link("jenkins", "Jenkins", "https://deploy-qa.tax.service.gov.uk/job/deploy-microservice"),
            Link("grafana", "Grafana", "https://grafana-datacentred-sal01-qa.tax.service.gov.uk/#/dashboard")
          )),
        Environment(
          "Production",
          Seq(
            Link("jenkins", "Jenkins", "https://deploy-prod.tax.service.gov.uk/job/deploy-microservice"),
            Link("grafana", "Grafana", "https://grafana-prod.tax.service.gov.uk/#/dashboard")
          )
        ))))

      responseData.repoType should ===(RepoType.Service)
    }
  }

  "allRepositories" should {
    "return all the repositories returned by the api" in {
      serviceEndpoint(GET, "/api/repositories", willRespondWith = (200, Some(JsonData.repositoriesData)))

      val repositories: Timestamped[Seq[RepositoryDisplayDetails]] = TeamsAndRepositoriesConnector.allRepositories(HeaderCarrier.fromHeadersAndSession(FakeHeaders())).futureValue

      repositories.data(0).name shouldBe "teamA-serv"
      repositories.data(0).createdAt shouldBe JsonData.createdAt
      repositories.data(0).lastUpdatedAt shouldBe JsonData.lastActiveAt
      repositories.data(0).repoType shouldBe RepoType.Service

      repositories.data(1).name shouldBe "teamB-library"
      repositories.data(1).createdAt shouldBe JsonData.createdAt
      repositories.data(1).lastUpdatedAt shouldBe JsonData.lastActiveAt
      repositories.data(1).repoType shouldBe RepoType.Library

      repositories.data(2).name shouldBe "teamB-other"
      repositories.data(2).createdAt shouldBe JsonData.createdAt
      repositories.data(2).lastUpdatedAt shouldBe JsonData.lastActiveAt
      repositories.data(2).repoType shouldBe RepoType.Other

    }
  }


}
