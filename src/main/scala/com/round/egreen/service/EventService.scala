// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.round.egreen.common.districtTMap
import com.round.egreen.common.model._
import com.round.egreen.cqrs.event._
import com.round.egreen.repository.{EventRepository, UserRepository}

final class EventService[F[_]](repo: EventRepository[F], userRepo: UserRepository[F]) {

  def createUser(user: User)(implicit F: Effect[F]): EitherT[F, String, User] =
    for {
      _ <- repo.saveEvent(
            CreateUser(
              user.id.some,
              user.username.some,
              user.encryptedPassword.some,
              user.roles.toSeq
            ).envelope
          )
      _ <- userRepo.putUser(user)
    } yield user

  def updateCustomer(customerInfo: CustomerInfo)(implicit F: Effect[F]): EitherT[F, String, CustomerInfo] =
    for {
      _ <- repo.saveEvent(
            UpdateCustomer(
              customerInfo.userId.some,
              customerInfo.fullName.some,
              customerInfo.phoneNumber.some,
              customerInfo.address.some,
              districtTMap.toBase(customerInfo.district).some
            ).envelope
          )
      _ <- userRepo.putCustomerInfo(customerInfo)
    } yield customerInfo

  def createPackage(p: ProductPackage)(implicit F: Effect[F]): EitherT[F, String, ProductPackage] =
    for {
      _ <- repo.saveEvent(
            CreateProductPackage(p.id.some, p.amount.some, p.frequency.some, p.price.some).envelope
          )
    } yield p

  def updatePackage(p: ProductPackage)(implicit F: Effect[F]): EitherT[F, String, ProductPackage] =
    for {
      _ <- repo.saveEvent(
            UpdateProductPackage(p.id.some, p.active.some, p.amount.some, p.frequency.some, p.price.some).envelope
          )
    } yield p

  def createSubscription(s: ProductSubscription)(implicit F: Effect[F]): EitherT[F, String, ProductSubscription] =
    for {
      _ <- repo.saveEvent(
            CreateProductSubscription(
              s.id.some,
              s.packageId.some,
              s.customerId.some,
              s.startWeek.some,
              s.endWeek.some,
              s.totalAmount.some
            ).envelope
          )
    } yield s
}
