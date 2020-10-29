package com.reactivebbq.loyalty

import org.scalatest.wordspec.AnyWordSpec

class LoyaltyInformationTest extends AnyWordSpec {

  "currentTotal" should {
    "return zero if there are no transactions" in {
      val loyalty = LoyaltyInformation.empty

      assert(loyalty.currentTotal === 0)
    }
    "return the value of an award if it is the only transaction" in {
      val loyalty = LoyaltyInformation(Seq(Award(10)))

      assert(loyalty.currentTotal === 10)
    }
    "return a negative value of a deduct if it is the only transaction" in {
      val loyalty = LoyaltyInformation(Seq(Deduct(10)))

      assert(loyalty.currentTotal === -10)
    }
    "return the combined value of all transactions" in {
      val loyalty = LoyaltyInformation(Seq(
        Award(100),
        Deduct(50),
        Award(30),
        Deduct(20)
      ))

      assert(loyalty.currentTotal === 60)
    }
  }

  "applyAdjustment" should {
    "Add the adjustment to the list" in {
      val loyalty = LoyaltyInformation.empty.applyAdjustment(Award(10))

      assert(loyalty.adjustments === Seq(Award(10)))
    }
    "Apply multiple adjustments" in {
      val loyalty = LoyaltyInformation.empty
        .applyAdjustment(Award(10))
        .applyAdjustment(Deduct(20))
        .applyAdjustment(Award(30))

      assert(loyalty.adjustments === Seq(Award(10), Deduct(20), Award(30)))
    }
  }

}
