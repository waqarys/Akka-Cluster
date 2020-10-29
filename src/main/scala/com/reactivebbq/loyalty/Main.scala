package com.reactivebbq.loyalty

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.management.scaladsl.AkkaManagement
import org.slf4j.LoggerFactory

object Main extends App {
  val log = LoggerFactory.getLogger(this.getClass)

  val Opt = """-D(\S+)=(\S+)""".r
  args.toList.foreach {
    case Opt(key, value) =>
      log.info(s"Config Override: $key = $value")
      System.setProperty(key, value)
  }

  implicit val system: ActorSystem = ActorSystem("Loyalty")

  AkkaManagement(system).start()

  val rootPath = Paths.get("tmp")
  val loyaltyRepository: LoyaltyRepository = new FileBasedLoyaltyRepository(rootPath)(system.dispatcher)

  //val loyaltyActorSupervisor = system.actorOf(LoyaltyActorSupervisor.props(loyaltyRepository))

  // TODO: Uncomment to enable cluster sharding.
  val loyaltyActorSupervisor = ClusterSharding(system).start(
    "loyalty",
    LoyaltyActor.props(loyaltyRepository),
    ClusterShardingSettings(system),
    LoyaltyActorSupervisor.idExtractor,
    LoyaltyActorSupervisor.shardIdExtractor
  )

  val loyaltyRoutes = new LoyaltyRoutes(loyaltyActorSupervisor)(system.dispatcher)

  Http().bindAndHandle(loyaltyRoutes.routes, "localhost")
}
