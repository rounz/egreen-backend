// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.http

import cats.effect.Effect
import cats.implicits._
import com.round.egreen.service.CoreService
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`

class CoreHttp[F[_]: Effect](coreService: CoreService[F]) extends Http4sDsl[F] {

  val service: HttpService[F] = {
    HttpService[F] {
      case GET -> Root / "health" =>
        for {
          health   <- coreService.health
          response <- Ok(health, `Content-Type`(MediaType.`application/json`))
        } yield response

      case GET -> Root / "version" =>
        for {
          buildInfo <- coreService.buildInfo
          response  <- Ok(buildInfo, `Content-Type`(MediaType.`application/json`))
        } yield response
    }
  }
}
