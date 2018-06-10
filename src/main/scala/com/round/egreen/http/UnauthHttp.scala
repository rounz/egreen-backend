// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.http

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.round.egreen.cqrs.command
import com.round.egreen.service.{UserAuth, UserService}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization

class UnauthHttp[F[_]: Effect](userAuth: UserAuth, userService: UserService[F]) extends Http4sDsl[F] {

  val service: HttpService[F] = HttpService[F] {
    case GET -> Root / username =>
      userService.checkUserExists(username) >>= {
        if (_) Conflict() else Ok()
      }

    case request @ POST -> Root =>
      (for {
        cmd <- EitherT.right(request.as[command.UserLogin])
        user <- userService
                 .getUser(cmd.username)
                 .leftMap(_ => Response[F](Unauthorized))
                 .ensure(Response[F](Unauthorized))(_.encryptedPassword == cmd.encryptedPassword)
        response <- EitherT.right[Response[F]](
                     Ok(
                       user.asJson,
                       Authorization(Credentials.Token(AuthScheme.Bearer, userAuth.authToken(user)))
                     )
                   )
      } yield response).merge
  }
}
