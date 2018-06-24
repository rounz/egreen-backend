// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.round.egreen.common.model._
import com.round.egreen.cqrs.event._
import com.round.egreen.repository._

class ReplayService[F[_]](userRepo: UserRepository[F],
                          productRepo: ProductRepository[F],
                          purchaseRepo: PurchaseRepository[F]) {
  import ReplayService._

  def replay(event: EventEnvelope)(implicit F: Effect[F]): EitherT[F, String, Boolean] =
    EitherT.fromOption[F](event.eventName, UNKNOWN_EVENT) >>= { eventName =>
      if (event.getEventName == CreateUser.eventName) {
        for {
          e    <- EitherT.rightT[F, String](CreateUser.parseFrom(event.getPayload.toByteArray))
          user <- EitherT.rightT[F, String](User(e.getId, e.getUsername, e.getEncryptedPassword, e.roles.toSet))
          _    <- userRepo.putUser(user)
        } yield true

      } else if (event.getEventName == CreateProductPackage.eventName) {
        for {
          e <- EitherT.rightT[F, String](CreateProductPackage.parseFrom(event.getPayload.toByteArray))
          p <- EitherT.rightT[F, String](ProductPackage(e.getId, true, e.getAmount, e.getFrequency, e.getPrice))
          _ <- productRepo.putPackage(p)
        } yield true

      } else if (event.getEventName == UpdateProductPackage.eventName) {
        for {
          e <- EitherT.rightT[F, String](UpdateProductPackage.parseFrom(event.getPayload.toByteArray))
          p <- EitherT.rightT[F, String](ProductPackage(e.getId, e.getActive, e.getAmount, e.getFrequency, e.getPrice))
          _ <- productRepo.putPackage(p)
        } yield true

      } else if (event.getEventName == CreateProductSubscription.eventName) {
        for {
          e <- EitherT.rightT[F, String](CreateProductSubscription.parseFrom(event.getPayload.toByteArray))
          s <- EitherT.rightT[F, String](
                ProductSubscription(e.getId,
                                    e.getCustomerId,
                                    e.getPackageId,
                                    e.getStartWeek,
                                    e.getEndWeek,
                                    e.getTotalAmount,
                                    e.getTotalAmount)
              )
          _ <- purchaseRepo.putSubscription(s)
        } yield true

      } else {
        EitherT.leftT(UNKNOWN_EVENT)
      }
    }
}

object ReplayService {
  val UNKNOWN_EVENT: String = "event.unknown"
}
