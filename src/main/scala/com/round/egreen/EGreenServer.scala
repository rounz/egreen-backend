// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen

import cats.effect.{Effect, IO}
import cats.implicits._
import com.round.egreen.module._
import com.round.egreen.repository._
import com.round.egreen.service._
import com.typesafe.config.{Config, ConfigFactory}
import fs2.{Stream, StreamApp}
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

object EGreenServer extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] =
    ServerStream.stream[IO](requestShutdown)
}

object ServerStream {

  def replayStream[F[_]: Effect](eventRepo: EventRepository[F],
                                 userRepo: UserRepository[F],
                                 requestShutdown: F[Unit]): Stream[F, StreamApp.ExitCode] = {
    val replayService: ReplayService[F] = new ReplayService[F](userRepo)
    eventRepo.eventStream.evalMap(
      replayService
        .replay(_)
        .map(_ => StreamApp.ExitCode.Success)
        .leftSemiflatMap(_ => requestShutdown.map(_ => StreamApp.ExitCode.Error))
        .merge
    )
  }

  def stream[F[_]: Effect](requestShutdown: F[Unit])(implicit ec: ExecutionContext): Stream[F, StreamApp.ExitCode] = {
    val config: Config             = ConfigFactory.load()
    val mongodbModule: MongoModule = new MongoModule(config)
    val redisModule: RedisModule   = new RedisModule(config)
    val userAuth: UserAuth         = new UserAuth(config)
    val httpModule: HttpModule[F]  = new HttpModule(mongodbModule, redisModule, userAuth, config)

    val serverStream = BlazeBuilder[F]
      .bindHttp(config.getInt("http.port"), "0.0.0.0")
      .mountService(httpModule.coreService <+> httpModule.frontendService, "/")
      .mountService(httpModule.unauthService, "/auth")
      .mountService(httpModule.commandService, "/c")
      .serve

    if (config.getBoolean("application.replay")) {
      replayStream(httpModule.eventRepo, httpModule.userRepo, requestShutdown).drain ++
      serverStream
    } else {
      serverStream
    }
  }
}
