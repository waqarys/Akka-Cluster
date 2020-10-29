package com.reactivebbq.loyalty

import java.io._
import java.nio.file.{Files, Path, Paths}

import akka.Done

import scala.concurrent.{ExecutionContext, Future}

trait LoyaltyRepository {
  def updateLoyalty(
    loyaltyId: LoyaltyId,
    loyaltyInformation: LoyaltyInformation): Future[Done]

  def findLoyalty(loyaltyId: LoyaltyId): Future[LoyaltyInformation]
}

class InMemoryLoyaltyRepository(implicit ec: ExecutionContext)
  extends LoyaltyRepository {
  var data: Map[LoyaltyId, LoyaltyInformation] = Map.empty

  override def updateLoyalty(
    loyaltyId: LoyaltyId,
    loyaltyInformation: LoyaltyInformation): Future[Done] = Future {
    data = data.updated(loyaltyId, loyaltyInformation)
    Done
  }

  override def findLoyalty(loyaltyId: LoyaltyId)
  : Future[LoyaltyInformation] = Future {
    data(loyaltyId)
  }
}

class FileBasedLoyaltyRepository(rootPath: Path)(implicit ec: ExecutionContext)
  extends LoyaltyRepository {

  rootPath.toFile.mkdirs()

  override def updateLoyalty(
    loyaltyId: LoyaltyId,
    loyaltyInformation: LoyaltyInformation
  ): Future[Done] = {
    Future {
      val file = new File(rootPath.toFile, loyaltyId.value)

      Files.write(
        Paths.get(file.getAbsolutePath),
        loyaltyInformation.adjustments.map(_.absoluteValue).mkString(",").getBytes
      )

      Done
    }
  }


  override def findLoyalty(loyaltyId: LoyaltyId): Future[LoyaltyInformation]
  = {
    Future {
      val file = new File(rootPath.toFile, loyaltyId.value)

      val fileContents = new String(
        Files.readAllBytes(Paths.get(file.getAbsolutePath))
      )

      val adjustments = fileContents.split(",").toSeq.map(_.toInt).map {
        case points if points >= 0 => Award(points)
        case points if points < 0 => Deduct(-points)
      }

      LoyaltyInformation(adjustments)
    }
  }
}
