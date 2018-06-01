// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import cats.effect.Sync
import com.round.egreen.build.BuildInfo
import io.circe.Json
import io.circe.parser._

final class CoreService[F[_]] {

  def health(implicit S: Sync[F]): F[Json] =
    S.pure(Json.obj("status" -> Json.fromString("OK")))

  def buildInfo(implicit S: Sync[F]): F[Json] =
    S.pure(parse(BuildInfo.toJson).fold(throw _, identity))
}
