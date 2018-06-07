// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import cats.data.EitherT
import cats.effect.Effect
import com.round.egreen.common.model.User
import com.round.egreen.cqrs.event
import com.round.egreen.repository.{EventRepository, UserRepository}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._

final class EventService[F[_]](repo: EventRepository[F], userRepo: UserRepository[F]) {

  def createUser(evt: event.CreateUser)(implicit F: Effect[F]): EitherT[F, String, Json] =
    for {
      _ <- repo.saveEvent(evt.envelope)
      user = User(evt.username, evt.encryptedPassword, evt.roles)
      _ <- userRepo.putUser(user)
    } yield user.asJson
}
