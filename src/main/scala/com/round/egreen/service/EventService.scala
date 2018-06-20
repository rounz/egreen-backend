// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.round.egreen.common.districtTMap
import com.round.egreen.common.model.{CustomerInfo, User}
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
}
