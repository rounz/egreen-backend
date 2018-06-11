// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import java.time.Instant

import cats.data.{Kleisli, OptionT}
import cats.effect.Effect
import com.round.egreen.common.model._
import com.typesafe.config.Config
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce}

class UserAuth(config: Config) {
  import AuthScheme.Bearer
  import Credentials.Token
  import UserAuth._

  def apply[F[_]: Effect](service: AuthedService[UserClaim, F]): HttpService[F] =
    AuthMiddleware(authUser).apply(service)

  def admin[F[_]: Effect](service: AuthedService[UserClaim, F]): HttpService[F] =
    AuthMiddleware(authAdmin).apply(service)

  def authToken(user: User): String = {
    val claim = UserClaim(
      user.username,
      user.roles,
      Instant.now.plusSeconds(157784760).getEpochSecond,
      Instant.now.getEpochSecond
    ).asJson
    JwtCirce.encode(TOKEN_HEADER, claim, secret)
  }

  private val secret: String = config.getString("application.secret")

  private def authUser[F[_]: Effect]: Kleisli[OptionT[F, ?], Request[F], UserClaim] =
    Kleisli { request =>
      OptionT.fromOption[F] {
        for {
          Authorization(Token(Bearer, token)) <- request.headers.get(Authorization)
          json                                <- JwtCirce.decodeJson(token, secret, algorithm :: Nil).toOption
          u @ UserClaim(_, _, expr, _)        <- json.as[UserClaim].toOption if Instant.now.getEpochSecond <= expr
        } yield u
      }
    }

  private def authAdmin[F[_]: Effect]: Kleisli[OptionT[F, ?], Request[F], UserClaim] =
    authRole(Admin)

  private def authRole[F[_]: Effect](role: Role): Kleisli[OptionT[F, ?], Request[F], UserClaim] =
    authUser.andThen { u =>
      OptionT.fromOption(
        Some(u).filter(_.roles.contains(role))
      )
    }
}

object UserAuth {

  final case class UserClaim(username: String, roles: Set[Role], expiration: Long, issuedAt: Long)

  val algorithm: JwtHmacAlgorithm = JwtAlgorithm.HS256

  val TOKEN_HEADER: Json = Json.obj(
    "typ" -> Json.fromString("JWT"),
    "alg" -> Json.fromString(algorithm.name)
  )
}
