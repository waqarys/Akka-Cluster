package com.reactivebbq.loyalty

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId}

object LoyaltyActorSupervisor {
  case class Deliver(command: LoyaltyActor.Command, to: LoyaltyId) extends SerializableMessage

  def props(loyaltyRepository: LoyaltyRepository): Props =
    Props(new LoyaltyActorSupervisor(loyaltyRepository))

  val idExtractor: ExtractEntityId = {
    case Deliver(msg, id) => (id.value.toString, msg)
  }

  val shardIdExtractor: ExtractShardId = {
    case Deliver(_, id) =>
      (Math.abs(id.value.hashCode) % 30).toString
  }
}

class LoyaltyActorSupervisor(loyaltyRepository: LoyaltyRepository) extends Actor {
  import LoyaltyActorSupervisor._

  protected def createLoyaltyActor(name: String): ActorRef = {
    context.actorOf(LoyaltyActor.props(loyaltyRepository), name)
  }

  override def receive: Receive = {
    case Deliver(command, to) =>
      val loyaltyActor = context.child(to.value)
        .getOrElse(createLoyaltyActor(to.value))

      loyaltyActor.forward(command)
  }
}
