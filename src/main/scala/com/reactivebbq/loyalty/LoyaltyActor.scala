package com.reactivebbq.loyalty

import akka.actor.{Actor, ActorLogging, Props, Stash}
import akka.pattern.pipe

object LoyaltyActor {
  sealed trait Command extends SerializableMessage
  sealed trait Event extends SerializableMessage

  case class ApplyLoyaltyAdjustment(adjustment: LoyaltyAdjustment) extends Command
  case class LoyaltyAdjustmentApplied(adjustment: LoyaltyAdjustment) extends Event
  case class LoyaltyAdjustmentRejected(adjustment: LoyaltyAdjustment, reason: String) extends Event

  case class GetLoyaltyInformation() extends Command

  def props(loyaltyRepository: LoyaltyRepository): Props =
    Props(new LoyaltyActor(loyaltyRepository))
}

class LoyaltyActor(loyaltyRepository: LoyaltyRepository)
  extends Actor
    with Stash
    with ActorLogging {
  import LoyaltyActor._
  import context.dispatcher

  private val loyaltyId = LoyaltyId(self.path.name)
  private var loyaltyInformation = LoyaltyInformation.empty

  override def preStart(): Unit = {
    super.preStart()

    loyaltyRepository.findLoyalty(loyaltyId).map {
      loyaltyInfo =>
        log.info(s"Loyalty Information Loaded For ${loyaltyId.value}")
        loyaltyInfo
    }.recover {
      case _ =>
        log.info(s"Creating New Loyalty Account For ${loyaltyId.value}.")
        LoyaltyInformation.empty
    }.pipeTo(self)
  }

  override def receive: Receive = initializing

  private def initializing: Receive = {
    case loyaltyInfo: LoyaltyInformation =>
      loyaltyInformation = loyaltyInfo
      context.become(running)
      unstashAll()
    case _ =>
      stash()
  }

  private def running: Receive = {
    case cmd: ApplyLoyaltyAdjustment => handle(cmd)
    case GetLoyaltyInformation() =>
      log.info(s"Retrieving Loyalty Information For ${loyaltyId.value}")
      sender() ! loyaltyInformation
  }

  private def handle(cmd: ApplyLoyaltyAdjustment) = {
    cmd match {
      case ApplyLoyaltyAdjustment(Deduct(points)) if points > loyaltyInformation.currentTotal =>
        log.info(s"Insufficient Points For ${loyaltyId.value}")
        sender() ! LoyaltyAdjustmentRejected(Deduct(points), "Insufficient Points")
      case ApplyLoyaltyAdjustment(adjustment) =>
        log.info(s"Applying $adjustment for ${loyaltyId.value}")
        loyaltyInformation = loyaltyInformation.applyAdjustment(adjustment)
        loyaltyRepository.updateLoyalty(loyaltyId, loyaltyInformation).map { _ =>
          LoyaltyAdjustmentApplied(adjustment)
        }.pipeTo(sender())
    }
  }
}
