// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.http

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.round.egreen.cqrs.command
import com.round.egreen.service.UserService
import io.circe.{Decoder, Json}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class CommandHttp[F[_]: Effect](userService: UserService[F]) extends Http4sDsl[F] {
  import CommandHttp._

  val service: HttpService[F] = {
    HttpService[F] {
      case request @ POST -> Root =>
        for {
          cmd <- request.as[command.CommandEnvelope]
          json <- (if (cmd.commandName == command.CreateUser.commandName) {
                     for {
                       userCmd <- parseCommand[command.CreateUser](cmd.json)
                       json    <- userService.createUser(userCmd)
                     } yield json
                   } else {
                     EitherT.leftT[F, Json](COMMAND_NOT_SUPPORTED)
                   }).value
          response <- json.fold(
                       e => BadRequest(s"{ error: $e }".asJson),
                       Ok(_)
                     )
        } yield response
    }
  }

  private def parseCommand[C: Decoder](json: String): EitherT[F, String, C] =
    EitherT.fromEither[F](
      parse(json).flatMap(_.as[C]).leftMap(_.toString)
    )
}

object CommandHttp {
  val COMMAND_NOT_SUPPORTED = "command.not-supported"
}
