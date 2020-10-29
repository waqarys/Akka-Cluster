package com.reactivebbq.loyalty

import java.nio.file.{Files, Path}
import java.util.UUID

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global

class InMemoryLoyaltyRepositoryTest extends LoyaltyRepositoryTest {
  override val loyaltyRepository: LoyaltyRepository =
    new InMemoryLoyaltyRepository()
}

class FileBasedLoyaltyRepositoryTest
  extends LoyaltyRepositoryTest
    with BeforeAndAfterAll {
  val tmpDir: Path = Files.createTempDirectory("filebasedrepotest")
  tmpDir.toFile.deleteOnExit()

  override val loyaltyRepository: LoyaltyRepository =
    new FileBasedLoyaltyRepository(tmpDir)

  override protected def afterAll(): Unit = {
    super.afterAll()
    tmpDir.toFile.listFiles().foreach(_.deleteOnExit())
    tmpDir.toFile.deleteOnExit()
  }
}

trait LoyaltyRepositoryTest extends AnyWordSpec with ScalaFutures {
  val loyaltyRepository: LoyaltyRepository

  def createLoyaltyId(): LoyaltyId = LoyaltyId(UUID.randomUUID.toString)

  "findLoyalty" should {
    "return nothing if the id does not exist" in {
      val result = loyaltyRepository.findLoyalty(createLoyaltyId())

      intercept[Exception] {
        result.futureValue
      }
    }
    "return the loyalty if it exists" in {
      val info = LoyaltyInformation(Seq(Award(10), Deduct(5)))
      val id = createLoyaltyId()

      loyaltyRepository.updateLoyalty(id, info).futureValue

      val result = loyaltyRepository.findLoyalty(id)

      assert(result.futureValue === info)
    }
    "return the correct loyalty if multiples exist" in {
      val info1 = LoyaltyInformation(Seq(Award(10), Deduct(5)))
      val id1 = createLoyaltyId()

      val info2 = LoyaltyInformation(Seq(Award(5), Deduct(3)))
      val id2 = createLoyaltyId()

      loyaltyRepository.updateLoyalty(id1, info1).futureValue
      loyaltyRepository.updateLoyalty(id2, info2).futureValue

      val result = loyaltyRepository.findLoyalty(id1)

      assert(result.futureValue === info1)
    }
  }

  "updateLoyalty" should {
    "overwrite an existing value" in {
      val info1 = LoyaltyInformation(Seq(Award(10), Deduct(5)))
      val info2 = LoyaltyInformation(Seq(Award(5), Deduct(3)))
      val id = createLoyaltyId()

      loyaltyRepository.updateLoyalty(id, info1).futureValue
      loyaltyRepository.updateLoyalty(id, info2).futureValue

      val result = loyaltyRepository.findLoyalty(id)

      assert(result.futureValue === info2)
    }
  }
}
