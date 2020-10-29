package com.reactivebbq.loyalty

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestActor, TestProbe}
import com.reactivebbq.loyalty.LoyaltyActorSupervisor.Deliver
import org.scalatest.wordspec.AnyWordSpec

class LoyaltyRoutesTest
  extends AnyWordSpec with ScalatestRouteTest {

  val loyaltyActorSupervisor = TestProbe()
  val routes = new LoyaltyRoutes(loyaltyActorSupervisor.ref)

  private def setAutoPilot(message: Any, response: Any): Unit = {
    loyaltyActorSupervisor.setAutoPilot(
      (sender, msg) => {
        msg match {
          case `message` =>
            sender ! response
            TestActor.KeepRunning
        }
      }
    )
  }

  "/loyalty/{id}" should {
    "Return the results from the supervisor" in {
      val loyaltyId = LoyaltyId("someId")
      val expectedRequest = Deliver(
        LoyaltyActor.GetLoyaltyInformation(),
        loyaltyId
      )
      val expectedResponse = s"""Current Balance: 10\nHistory:\n- Award(10)"""

      setAutoPilot(expectedRequest, LoyaltyInformation(Seq(Award(10))))

      Get(s"/loyalty/${loyaltyId.value}") ~> routes.routes ~> check {
        assert(status === StatusCodes.OK)
        assert(contentType === ContentTypes.`text/plain(UTF-8)`)
        assert(entityAs[String] === expectedResponse)
      }
    }
  }

  "/loyalty/{id}/award/{points}" should {
    "Indicate the adjustment was applied if it succeeds." in {
      val loyaltyId = LoyaltyId("someId")
      val points = 30
      val expectedRequest = Deliver(
        LoyaltyActor.ApplyLoyaltyAdjustment(Award(points)),
        loyaltyId
      )
      val expectedResponse = LoyaltyActor.LoyaltyAdjustmentApplied(Award(points))

      setAutoPilot(expectedRequest, expectedResponse)

      Post(s"/loyalty/${loyaltyId.value}/award/$points") ~> routes.routes ~> check {
        assert(status === StatusCodes.OK)
        assert(contentType === ContentTypes.`text/plain(UTF-8)`)
        assert(entityAs[String] === s"Applied: ${expectedResponse.adjustment}")
      }
    }
    "Indicate the adjustment was not applied if it failed." in {
      val loyaltyId = LoyaltyId("someId")
      val points = 30
      val expectedRequest = Deliver(
        LoyaltyActor.ApplyLoyaltyAdjustment(Award(points)),
        loyaltyId
      )
      val expectedResponse = LoyaltyActor.LoyaltyAdjustmentRejected(Award(points), "reason")

      setAutoPilot(expectedRequest, expectedResponse)

      Post(s"/loyalty/${loyaltyId.value}/award/$points") ~> routes.routes ~> check {
        assert(status === StatusCodes.BadRequest)
        assert(contentType === ContentTypes.`text/plain(UTF-8)`)
        assert(entityAs[String] === s"Rejected: ${expectedResponse.reason}")
      }
    }
  }

  "/loyalty/{id}/deduct/{points}" should {
    "Indicate the adjustment was applied if it succeeds." in {
      val loyaltyId = LoyaltyId("someId")
      val points = 30
      val expectedRequest = Deliver(
        LoyaltyActor.ApplyLoyaltyAdjustment(Deduct(points)),
        loyaltyId
      )
      val expectedResponse = LoyaltyActor.LoyaltyAdjustmentApplied(Deduct(points))

      setAutoPilot(expectedRequest, expectedResponse)

      Post(s"/loyalty/${loyaltyId.value}/deduct/$points") ~> routes.routes ~> check {
        assert(status === StatusCodes.OK)
        assert(contentType === ContentTypes.`text/plain(UTF-8)`)
        assert(entityAs[String] === s"Applied: ${expectedResponse.adjustment}")
      }
    }
    "Indicate the adjustment was rejected if it fails." in {
      val loyaltyId = LoyaltyId("someId")
      val points = 30
      val expectedRequest = Deliver(
        LoyaltyActor.ApplyLoyaltyAdjustment(Deduct(points)),
        loyaltyId
      )
      val expectedResponse = LoyaltyActor.LoyaltyAdjustmentRejected(Deduct(points), "reason")

      setAutoPilot(expectedRequest, expectedResponse)

      Post(s"/loyalty/${loyaltyId.value}/deduct/$points") ~> routes.routes ~> check {
        assert(status === StatusCodes.BadRequest)
        assert(contentType === ContentTypes.`text/plain(UTF-8)`)
        assert(entityAs[String] === s"Rejected: ${expectedResponse.reason}")
      }
    }
  }

}
