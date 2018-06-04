// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen

import cats.effect.{Effect, IO}
import com.round.egreen.http.CoreHttp
import com.round.egreen.service.CoreService
import fs2.StreamApp
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext

object EGreenServer extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]) = ServerStream.stream[IO]
}

object ServerStream {

  def coreService[F[_]: Effect] = new CoreHttp[F](new CoreService).service

  def stream[F[_]: Effect](implicit ec: ExecutionContext) =
    BlazeBuilder[F]
      .bindHttp(System.getenv("PORT").toInt, "0.0.0.0")
      .mountService(coreService, "/")
      .serve
}
