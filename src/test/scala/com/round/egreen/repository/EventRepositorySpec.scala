// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.repository

import java.util.UUID

import cats.effect.IO
import cats.implicits._
import com.round.egreen.common.model._
import com.round.egreen.cqrs.event.CreateUser
import com.round.egreen.module.MongoModule
import com.typesafe.config.{Config, ConfigFactory}
import org.mongodb.scala._
import org.scalatest._

class EventRepositorySpec extends WordSpec with Matchers with BeforeAndAfterAll {
  private val config: Config                             = ConfigFactory.load()
  private val mongoModule: MongoModule                   = new MongoModule(config)
  private val eventCollection: MongoCollection[Document] = mongoModule.eventCollection
  private val repo: EventRepository[IO]                  = new MongoEventRepository(eventCollection)

  "MongoEventRepository" should {
    "save and fetch events correctly" in {
      val event    = CreateUser(UUID.randomUUID().some, "abc".some, None, Admin :: Customer :: Nil)
      val envelope = event.envelope
      repo.saveEvent(envelope).value.unsafeRunSync()
      val Vector(e) = repo.eventStream.compile.toVector.unsafeRunSync()
      e shouldBe envelope
    }
  }

  override protected def afterAll(): Unit = {
    import org.mongodb.scala.model.Filters._
    IO.fromFuture(IO(eventCollection.deleteMany(exists("_id")).toFuture)).unsafeRunSync()
    mongoModule.mongoClient.close()
  }
}
