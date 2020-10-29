package com.reactivebbq.loyalty

import java.util.UUID

import akka.Done
import akka.actor.ActorRef
import akka.pattern.ask
import com.reactivebbq.loyalty.LoyaltyActor.LoyaltyAdjustmentRejected
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class LoyaltyActorTest
  extends AnyWordSpec
    with AkkaSpec
    with ScalaFutures {

  class BrokenLoyaltyRepository extends LoyaltyRepository {
    override def updateLoyalty(
      loyaltyId: LoyaltyId,
      loyaltyInformation: LoyaltyInformation): Future[Done] = {
      Future.failed(new Exception("Boom"))
    }

    override def findLoyalty(loyaltyId: LoyaltyId): Future
      [LoyaltyInformation] = Future.failed(new Exception("Bam"))
  }

  class TestContext {
    val loyaltyId = LoyaltyId(UUID.randomUUID.toString)
    val loyaltyRepository = new InMemoryLoyaltyRepository
    val brokenRepository = new BrokenLoyaltyRepository

    lazy val loyaltyActor: ActorRef = system.actorOf(
      LoyaltyActor.props(loyaltyRepository),
      loyaltyId.value
    )
  }

  "The Actor" should {

    "Load it's state from the repo on startup" in new TestContext {
      val state = LoyaltyInformation(Seq(Award(10)))
      loyaltyRepository.updateLoyalty(loyaltyId, state)

      val result = (loyaltyActor ? LoyaltyActor.GetLoyaltyInformation())
        .mapTo[LoyaltyInformation]

      assert(result.futureValue === state)
    }
  }

  "ApplyLoyaltyAdjustment" should {

    "Return a corresponding event" in new TestContext {
      val result = (loyaltyActor ? LoyaltyActor.ApplyLoyaltyAdjustment(Award(10)))
        .mapTo[LoyaltyActor.LoyaltyAdjustmentApplied]

      assert(result.futureValue === LoyaltyActor.LoyaltyAdjustmentApplied(Award(10)))
    }

    "Apply all Loyalty Adjustments" in new TestContext {
      loyaltyActor ! LoyaltyActor.ApplyLoyaltyAdjustment(Award(10))
      loyaltyActor ! LoyaltyActor.ApplyLoyaltyAdjustment(Deduct(5))
      loyaltyActor ! LoyaltyActor.ApplyLoyaltyAdjustment(Award(20))

      val result = (loyaltyActor ? LoyaltyActor.GetLoyaltyInformation())
        .mapTo[LoyaltyInformation]

      assert(result.futureValue === LoyaltyInformation(Seq(
        Award(10),
        Deduct(5),
        Award(20)
      )))
    }

    "Update the Loyalty Repository" in new TestContext {
      (loyaltyActor ? LoyaltyActor.ApplyLoyaltyAdjustment(Award(10)))
        .mapTo[LoyaltyActor.LoyaltyAdjustmentApplied].futureValue

      assert(loyaltyRepository.findLoyalty(loyaltyId).futureValue === LoyaltyInformation(Seq(Award(10))))
    }

    "Fail to deduct if there is insufficient points" in new TestContext {
      (loyaltyActor ? LoyaltyActor.ApplyLoyaltyAdjustment(Award(10)))
        .mapTo[LoyaltyActor.LoyaltyAdjustmentApplied].futureValue

      val result = (loyaltyActor ? LoyaltyActor.ApplyLoyaltyAdjustment(Deduct(15)))
        .mapTo[LoyaltyActor.LoyaltyAdjustmentRejected].futureValue

      assert(result === LoyaltyAdjustmentRejected(Deduct(15), "Insufficient Points"))
    }

    "Fail if it can't write to the repo" in new TestContext {
      override lazy val loyaltyActor = system.actorOf(
        LoyaltyActor.props(brokenRepository)
      )

      val result = (loyaltyActor ? LoyaltyActor.ApplyLoyaltyAdjustment(Award(10)))
        .mapTo[LoyaltyActor.LoyaltyAdjustmentApplied]

      intercept[Exception] {
        result.futureValue
      }
    }
  }

  "GetLoyaltyInformation" should {

    "return empty LoyaltyInformation if no adjustments have been applied" in new TestContext{

      val result = (loyaltyActor ? LoyaltyActor.GetLoyaltyInformation())
        .mapTo[LoyaltyInformation]

      assert(result.futureValue === LoyaltyInformation.empty)
    }

    "return the updated LoyaltyInformation if an adjustment has been applied" in new TestContext {
      loyaltyActor ! LoyaltyActor.ApplyLoyaltyAdjustment(Award(10))

      val result = (loyaltyActor ? LoyaltyActor.GetLoyaltyInformation())
        .mapTo[LoyaltyInformation]

      assert(result.futureValue === LoyaltyInformation(Seq(Award(10))))
    }
  }
}
