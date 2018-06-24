// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import java.util.UUID

import cats.data.EitherT
import cats.effect.Effect
import com.round.egreen.common.model.ProductPackage
import com.round.egreen.cqrs.command.{CreateProductPackage, UpdateProductPackage}
import com.round.egreen.repository.ProductRepository

class ProductService[F[_]](eventService: EventService[F], repo: ProductRepository[F]) {

  def getAllPackages(implicit F: Effect[F]): EitherT[F, String, List[ProductPackage]] =
    repo.getAllPackages

  def getPackage(productId: UUID)(implicit F: Effect[F]): EitherT[F, String, ProductPackage] =
    repo.getPackage(productId)

  def createPackage(c: CreateProductPackage)(implicit F: Effect[F]): EitherT[F, String, ProductPackage] =
    for {
      p <- eventService.createPackage(ProductPackage(UUID.randomUUID(), true, c.amount, c.frequency, c.price))
      _ <- repo.putPackage(p)
    } yield p

  def updatePackage(c: UpdateProductPackage)(implicit F: Effect[F]): EitherT[F, String, Unit] =
    for {
      p <- eventService.updatePackage(ProductPackage(c.id, c.active, c.amount, c.frequency, c.price))
      _ <- repo.updatePackage(p)
    } yield ()
}
