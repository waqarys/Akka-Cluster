package com.reactivebbq.loyalty

import akka.pattern.ask
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec

class LoyaltyActorSupervisorTest
  extends AnyWordSpec
    with AkkaSpec
    with ScalaFutures {

  class TestContext {
    val loyaltyRepository = new InMemoryLoyaltyRepository
    val supervisor = system.actorOf(LoyaltyActorSupervisor.props(loyaltyRepository))
  }

  "Deliver" should {
    "create an actor and send it the command" in new TestContext {
      val id = LoyaltyId("Id")

      supervisor ! LoyaltyActorSupervisor.Deliver(
        LoyaltyActor.ApplyLoyaltyAdjustment(Award(10)),
        id
      )

      val result = (supervisor ? LoyaltyActorSupervisor.Deliver(
        LoyaltyActor.GetLoyaltyInformation(),
        id
      )).mapTo[LoyaltyInformation]

      assert(result.futureValue === LoyaltyInformation(Seq(Award(10))))
    }
    "reuse existing actors" in new TestContext {
      val id = LoyaltyId("Id")

      supervisor ! LoyaltyActorSupervisor.Deliver(
        LoyaltyActor.ApplyLoyaltyAdjustment(Award(10)),
        id
      )

      supervisor ! LoyaltyActorSupervisor.Deliver(
        LoyaltyActor.ApplyLoyaltyAdjustment(Deduct(5)),
        id
      )

      val result = (supervisor ? LoyaltyActorSupervisor.Deliver(
        LoyaltyActor.GetLoyaltyInformation(),
        id
      )).mapTo[LoyaltyInformation]

      assert(result.futureValue === LoyaltyInformation(Seq(Award(10), Deduct(5))))
    }
  }
}
