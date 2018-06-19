// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.round.egreen.common.model.User
import com.round.egreen.cqrs.event._
import com.round.egreen.repository.UserRepository

class ReplayService[F[_]](userRepo: UserRepository[F]) {
  import ReplayService._

  def replay(event: EventEnvelope)(implicit F: Effect[F]): EitherT[F, String, Boolean] =
    EitherT.fromOption[F](event.eventName, UNKNOWN_EVENT) >>= { eventName =>
      if (event.getEventName == CreateUser.eventName) {
        for {
          e    <- EitherT.rightT[F, String](CreateUser.parseFrom(event.getPayload.toByteArray))
          user <- EitherT.rightT[F, String](User(e.getId, e.getUsername, e.getEncryptedPassword, e.roles.toSet))
          _    <- userRepo.putUser(user)
        } yield {
          true
        }
      } else {
        EitherT.leftT(UNKNOWN_EVENT)
      }
    }
}

object ReplayService {
  val UNKNOWN_EVENT: String = "event.unknown"
}
