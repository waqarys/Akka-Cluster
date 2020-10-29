package com.reactivebbq.loyalty

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.ask
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.reactivebbq.loyalty.LoyaltyActor.{LoyaltyAdjustmentApplied, LoyaltyAdjustmentRejected}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class LoyaltyRoutes(loyaltyActors: ActorRef)(implicit ec: ExecutionContext) {
  private implicit val timeout: Timeout = Timeout(5.seconds)

  lazy val routes: Route =
    pathPrefix("loyalty") {
      pathPrefix(Segment) { id =>
        path("award" / IntNumber) { value =>
          post {
            val loyaltyId = LoyaltyId(id)
            val command = LoyaltyActor.ApplyLoyaltyAdjustment(Award(value))
            val result = (loyaltyActors ? LoyaltyActorSupervisor.Deliver(command, loyaltyId))
              .mapTo[LoyaltyActor.Event]

            onComplete(result) {
              case Success(LoyaltyAdjustmentApplied(adjustment)) =>
                complete(StatusCodes.OK, s"Applied: $adjustment")
              case Success(LoyaltyAdjustmentRejected(adjustment, reason)) =>
                complete(StatusCodes.BadRequest, s"Rejected: $reason")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, ex.getMessage)
            }
          }
        } ~
        path("deduct" / IntNumber) { value =>
          post {
            val loyaltyId = LoyaltyId(id)
            val command = LoyaltyActor.ApplyLoyaltyAdjustment(Deduct(value))
            val result = (loyaltyActors ? LoyaltyActorSupervisor.Deliver(command, loyaltyId))
              .mapTo[LoyaltyActor.Event]

            onComplete(result) {
              case Success(LoyaltyAdjustmentApplied(adjustment)) =>
                complete(StatusCodes.OK, s"Applied: $adjustment")
              case Success(LoyaltyAdjustmentRejected(adjustment, reason)) =>
                complete(StatusCodes.BadRequest, s"Rejected: $reason")
              case Failure(ex) =>
                complete(StatusCodes.InternalServerError, ex.getMessage)
            }
          }
        } ~
        pathEnd {
          get {
            val loyaltyId = LoyaltyId(id)
            val command = LoyaltyActor.GetLoyaltyInformation()
            val result = (loyaltyActors ? LoyaltyActorSupervisor.Deliver(command, loyaltyId))
              .mapTo[LoyaltyInformation]
              .map { info =>
                s"Current Balance: ${info.currentTotal}\nHistory:\n" + info.adjustments.mkString("- ", "\n- ", "")
              }

            complete(result)
          }
        }
      }
    }
}
