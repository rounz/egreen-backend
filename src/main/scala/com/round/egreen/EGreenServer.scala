// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen

import cats.effect.{Effect, IO}
import cats.syntax.semigroupk._
import com.round.egreen.module._
import com.typesafe.config.{Config, ConfigFactory}
import fs2.{Stream, StreamApp}
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

object EGreenServer extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] =
    ServerStream.stream[IO]
}

object ServerStream {

  def stream[F[_]: Effect](implicit ec: ExecutionContext): Stream[F, StreamApp.ExitCode] = {
    val config: Config            = ConfigFactory.load()
    val httpModule: HttpModule[F] = new HttpModule(config)

    BlazeBuilder[F]
      .bindHttp(config.getInt("http.port"), "0.0.0.0")
      .mountService(httpModule.coreService <+> httpModule.frontendService, "/")
      .mountService(httpModule.commandService, "/c")
      .serve
  }
}
