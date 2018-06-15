// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.round.egreen.common.model.User
import com.round.egreen.cqrs.event
import com.round.egreen.repository.{EventRepository, UserRepository}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._

final class EventService[F[_]](repo: EventRepository[F], userRepo: UserRepository[F]) {

  def createUser(user: User)(implicit F: Effect[F]): EitherT[F, String, Json] =
    for {
      _ <- repo.saveEvent(
            event
              .CreateUser(
                user.id.some,
                user.username.some,
                user.encryptedPassword.some,
                user.roles.toSeq
              )
              .envelope
          )
      _ <- userRepo.putUser(user)
    } yield user.asJson
}
