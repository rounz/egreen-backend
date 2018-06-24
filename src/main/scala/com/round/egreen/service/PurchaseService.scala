// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import java.util.UUID

import cats.data.EitherT
import cats.effect.Effect
import com.round.egreen.common.model.{Customer, ProductSubscription}
import com.round.egreen.cqrs.command.CreateProductSubscription
import com.round.egreen.repository.PurchaseRepository

class PurchaseService[F[_]](eventService: EventService[F],
                            userService: UserService[F],
                            productService: ProductService[F],
                            repo: PurchaseRepository[F]) {
  import PurchaseService._

  def getAllSubscriptions(implicit F: Effect[F]): EitherT[F, String, List[ProductSubscription]] =
    repo.getAllSubscriptions

  def createSubscription(c: CreateProductSubscription)(implicit F: Effect[F]): EitherT[F, String, Unit] =
    for {
      _ <- userService
            .getUser(c.customerId)
            .ensure(USER_NOT_CUSTOMER)(_.roles.contains(Customer))
      _ <- productService.getPackage(c.packageId)
      s <- eventService.createSubscription(
            ProductSubscription(UUID.randomUUID,
                                c.customerId,
                                c.packageId,
                                c.startWeek,
                                c.endWeek,
                                c.totalAmount,
                                c.totalAmount)
          )
      _ <- repo.putSubscription(s)
    } yield ()
}

object PurchaseService {
  val USER_NOT_CUSTOMER: String = "user.not-customer"
}
