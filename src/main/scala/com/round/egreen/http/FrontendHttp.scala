// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.http

import cats.effect.Effect
import org.http4s._
import org.http4s.dsl.Http4sDsl

class FrontendHttp[F[_]: Effect] extends Http4sDsl[F] {

  def static(file: String, request: Request[F]): F[Response[F]] =
    StaticFile
      .fromResource("/egreen-frontend-builds/" + file, Some(request))
      .getOrElseF(NotFound())

  val service: HttpService[F] = HttpService[F] {
    case request @ GET -> (Root / "admin" | Root / "admin" / "") =>
      static("admin/index.html", request)

    case request @ GET -> Root / "admin" / path =>
      static("admin/" + path, request)

    case request @ GET -> (Root / "developer" | Root / "developer" / "") =>
      static("developer/index.html", request)

    case request @ GET -> Root / "developer" / path =>
      static("developer/" + path, request)
  }
}
