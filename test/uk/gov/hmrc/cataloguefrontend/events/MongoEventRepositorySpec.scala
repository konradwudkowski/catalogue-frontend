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

import org.scalatest.FunSuite


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



import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, LoneElement, OptionValues}
import org.scalatestplus.play.OneAppPerTest
import reactivemongo.bson.{BSONDocument, BSONLong, BSONObjectID, BSONString}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import reactivemongo.json._

class MongoEventRepositorySpec extends UnitSpec with LoneElement with MongoSpecSupport with ScalaFutures with OptionValues with BeforeAndAfterEach with OneAppPerTest {


  val mongoEventRepository = new MongoEventRepository(mongo)

  override def beforeEach() {
    await(mongoEventRepository.drop)
  }


  "getAll" should {
    "return all the events" in {

      await(mongoEventRepository.collection.insert(Json.obj(
        "eventType" -> "ServiceOwnerUpdated" ,
        "eventData" ->
          Json.obj(
            "type" -> "ServiceOwnerUpdatedEventData",
            "data" -> Json.obj(
              "service" -> "Catalogue",
              "name" -> "Joe Black"
            )
          ),
        "timestamp" -> 1494625868
      )))

      val events: Seq[Event] = await(mongoEventRepository.getAllEvents)

      events.size shouldBe 1
      events.head shouldBe Event(EventType.ServiceOwnerUpdated, ServiceOwnerUpdatedEventData("Catalogue", "Joe Black"), 1494625868)
    }

//    "return all the deployments in descending order of productionDate" in {
//
//      val now: LocalDateTime = LocalDateTime.now()
//
//      await(mongoDeploymentsRepository.add(Deployment("test2", "v2", None, productionDate = now.minusDays(6))))
//      await(mongoDeploymentsRepository.add(Deployment("test3", "v3", None, productionDate = now.minusDays(5))))
//      await(mongoDeploymentsRepository.add(Deployment("test1", "v1", None, productionDate = now.minusDays(10))))
//      await(mongoDeploymentsRepository.add(Deployment("test4", "v4", None, productionDate = now.minusDays(2))))
//      await(mongoDeploymentsRepository.add(Deployment("test5", "vSomeOther1", None, now.minusDays(2), Some(1))))
//      await(mongoDeploymentsRepository.add(Deployment("test5", "vSomeOther2", None, now, Some(1), None, Seq(Deployer("xyz.abc", now)), None)))
//
//      val result: Seq[Deployment] = await(mongoDeploymentsRepository.getAllDeployments)
//
//      result.map(x => (x.name, x.version, x.deployers.map(_.name))) shouldBe Seq(
//        ("test5", "vSomeOther2", Seq("xyz.abc")),
//        ("test4", "v4", Nil),
//        ("test5", "vSomeOther1", Nil),
//        ("test3", "v3", Nil),
//        ("test2", "v2", Nil),
//        ("test1", "v1", Nil)
//      )
//
//    }
  }

  "getEventsByType" should {
    "return all the right events" in {
      val serviceOwnerUpdateEvent = Event(EventType.ServiceOwnerUpdated, ServiceOwnerUpdatedEventData("Catalogue", "Joe Black"), 1494625868)
      val otherEvent = Event(EventType.Other, ServiceOwnerUpdatedEventData("Catalogue", "Joe Black"), 1494625868)

      await(mongoEventRepository.add(serviceOwnerUpdateEvent))

      val events: Seq[Event] = await(mongoEventRepository.getEventsByType(EventType.ServiceOwnerUpdated))

      events.size shouldBe 1
      events.head shouldBe Event(EventType.ServiceOwnerUpdated, ServiceOwnerUpdatedEventData("Catalogue", "Joe Black"), 1494625868)
    }
  }

//  "getForService" should {
//    "return deployments for a service sorted in descending order of productionDate" in {
//      val now: LocalDateTime = LocalDateTime.now()
//
//      await(mongoDeploymentsRepository.add(Deployment("randomService", "vSomeOther1", None, now, Some(1))))
//      await(mongoDeploymentsRepository.add(Deployment("test", "v1", None, productionDate = now.minusDays(10), interval = Some(1))))
//      await(mongoDeploymentsRepository.add(Deployment("test", "v2", None, productionDate = now.minusDays(6), interval = Some(1))))
//      await(mongoDeploymentsRepository.add(Deployment("test", "v3", None, productionDate = now.minusDays(5), Some(1))))
//      await(mongoDeploymentsRepository.add(Deployment("test", "v4", None, productionDate = now.minusDays(2), Some(1))))
//
//      val deployments: Option[Seq[Deployment]] = await(mongoDeploymentsRepository.getForService("test"))
//
//      deployments.get.size shouldBe 4
//
//      deployments.get.map(_.version) shouldBe List("v4", "v3", "v2", "v1")
//
//    }
//  }


  "add" should {
    "be able to insert a new record and update it as well" in {
      val event = Event(EventType.ServiceOwnerUpdated, ServiceOwnerUpdatedEventData("Catalogue", "Joe Black"), 1494625868)
      await(mongoEventRepository.add(event))
      val all = await(mongoEventRepository.getAllEvents)

      all.size shouldBe 1
      val savedEvent: Event = all.loneElement

      savedEvent shouldBe event
    }
  }

//  "update" should {
//    "update already existing deployment" in {
//      val now: LocalDateTime = LocalDateTime.now()
//      await(mongoDeploymentsRepository.add(Deployment("test", "v", None, now)))
//
//      val all = await(mongoDeploymentsRepository.allServicedeployments)
//
//      val savedDeployment: Deployment = all.values.flatten.loneElement
//
//      await(mongoDeploymentsRepository.update(savedDeployment.copy(leadTime = Some(1))))
//
//      val allUpdated = await(mongoDeploymentsRepository.allServicedeployments)
//      allUpdated.size shouldBe 1
//      val updatedDeployment: Deployment = allUpdated.values.flatten.loneElement
//
//      updatedDeployment.name shouldBe savedDeployment.name
//      updatedDeployment.version shouldBe savedDeployment.version
//      updatedDeployment.creationDate shouldBe savedDeployment.creationDate
//      savedDeployment.productionDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) shouldBe savedDeployment.productionDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
//      updatedDeployment.leadTime shouldBe Some(1)
//    }
//
//  }

}
