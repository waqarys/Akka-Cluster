package com.reactivebbq.loyalty

case class LoyaltyId(value: String)

trait LoyaltyAdjustment extends SerializableMessage {
  def absoluteValue: Int
}

case class Award(points:Int) extends LoyaltyAdjustment {
  require(points > 0, "points must be a positive integer.")
  override def absoluteValue: Int = points
}
case class Deduct(points:Int) extends LoyaltyAdjustment {
  require(points > 0, "points must be a positive integer.")
  override def absoluteValue: Int = -points
}

object LoyaltyInformation {
  val empty = LoyaltyInformation(Seq.empty)
}

case class LoyaltyInformation(adjustments: Seq[LoyaltyAdjustment]) extends SerializableMessage {
  val currentTotal:Int = adjustments.foldLeft(0) {
    case (sum, adj) => sum + adj.absoluteValue
  }

  def applyAdjustment(adjustment: LoyaltyAdjustment): LoyaltyInformation = {
    this.copy(adjustments = adjustments :+ adjustment)
  }
}
