// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.module

import cats.effect.Effect
import com.redis.RedisClientPool
import com.round.egreen.cqrs.event.EventEnvelope
import com.round.egreen.http._
import com.round.egreen.repository._
import com.round.egreen.service._
import com.typesafe.config.Config
import org.bson.codecs.configuration.CodecRegistries._
import org.bson.codecs.configuration.CodecRegistry
import org.http4s.HttpService
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

class MongoModule(config: Config) {

  val mongoClient: MongoClient = MongoClient(config.getString("mongodb.uri"))
  val mongoDB: MongoDatabase   = mongoClient.getDatabase(config.getString("mongodb.dbname"))

  val codecRegistry: CodecRegistry = fromRegistries(
    fromProviders(classOf[EventEnvelope]),
    DEFAULT_CODEC_REGISTRY
  )

  val eventCollection: MongoCollection[EventEnvelope] = mongoDB
    .getCollection[EventEnvelope]("mongodb.event-collection")
    .withCodecRegistry(codecRegistry)
}

class RedisModule(config: Config) {

  val Array(_, _, pw, host, port) = config
    .getString("redis.url")
    .split(":")
    .flatMap(_.split("@"))

  val client: RedisClientPool = new RedisClientPool(
    host,
    port.toInt,
    secret = Some(pw)
  )
}

class HttpModule[F[_]: Effect](config: Config) {
  val mongodbModule: MongoModule = new MongoModule(config)
  val redisModule: RedisModule   = new RedisModule(config)

  val eventRepo: EventRepository[F] =
    new MongoEventRepository(mongodbModule.eventCollection)

  val userRepo: UserRepository[F] =
    new RedisUserRepository(redisModule.client)

  val eventService: EventService[F] =
    new EventService(eventRepo, userRepo)

  val userService: UserService[F] =
    new UserService(eventService, userRepo)

  val userAuth: UserAuth = new UserAuth(config)

  val unauthService: HttpService[F] =
    new UnauthHttp(userAuth, userService).service

  val commandService: HttpService[F] =
    new CommandHttp[F](userAuth, userService).service

  val coreService: HttpService[F] =
    new CoreHttp[F](new CoreService).service

  val frontendService: HttpService[F] =
    new FrontendHttp[F].service
}
