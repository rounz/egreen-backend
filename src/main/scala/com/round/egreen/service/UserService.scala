// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import cats.data.EitherT
import cats.effect.Effect
import com.round.egreen.common.model.User
import com.round.egreen.cqrs.command
import com.round.egreen.cqrs.event
import com.round.egreen.repository.UserRepository
import io.circe.Json

final class UserService[F[_]](eventService: EventService[F], repo: UserRepository[F]) {
  import UserRepository._
  import UserService._

  def createUser(cmd: command.CreateUser)(implicit F: Effect[F]): EitherT[F, String, Json] =
    for {
      _ <- repo
            .getUser(cmd.username)
            .ensure(USER_EXISTS)(_ => false)
            .recover {
              case USER_NOTFOUND =>
                User(cmd.username, cmd.encryptedPassword, cmd.roles)
            }
      json <- eventService.createUser(
               event.CreateUser(cmd.username, cmd.encryptedPassword, cmd.roles)
             )
    } yield json

}

object UserService {
  val USER_EXISTS = "user.exists"
}
