// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import java.util.UUID

import cats.data.EitherT
import cats.effect.Effect
import com.round.egreen.common.model._
import com.round.egreen.cqrs.command._
import com.round.egreen.repository.UserRepository

class UserService[F[_]](eventService: EventService[F], repo: UserRepository[F]) {
  import UserRepository._
  import UserService._

  def checkUserExists(username: String)(implicit F: Effect[F]): EitherT[F, String, Boolean] =
    repo.checkUserExists(username)

  def getUser(username: String)(implicit F: Effect[F]): EitherT[F, String, User] =
    repo.getUser(username)

  def getAllUsers(implicit F: Effect[F]): EitherT[F, String, List[User]] =
    repo.getAllUsers

  def createUser(cmd: CreateUser)(implicit F: Effect[F]): EitherT[F, String, User] =
    for {
      user <- repo
               .getUser(cmd.username)
               .ensure(USER_EXISTS)(_ => false)
               .recover {
                 case USER_NOTFOUND =>
                   User(UUID.randomUUID(), cmd.username, cmd.encryptedPassword, cmd.roles)
               }
      json <- eventService.createUser(user)
    } yield user

  def createCustomer(cmd: CreateCustomer)(implicit F: Effect[F]): EitherT[F, String, CustomerUser] =
    for {
      user <- createUser(CreateUser(cmd.username, cmd.encryptedPassword, Set(Customer)))
      info <- updateCustomer(
               UpdateCustomer(user.id,
                              user.username,
                              user.encryptedPassword,
                              cmd.fullName,
                              cmd.phoneNumber,
                              cmd.address,
                              cmd.district)
             )
    } yield CustomerUser(user, info)

  def getCustomer(cmd: GetCustomer)(implicit F: Effect[F]): EitherT[F, String, CustomerInfo] =
    repo.getCustomerInfo(cmd.userId)

  def updateCustomer(cmd: UpdateCustomer)(implicit F: Effect[F]): EitherT[F, String, CustomerInfo] =
    for {
      customerInfo <- eventService.updateCustomer(
                       CustomerInfo(cmd.id, cmd.fullName, cmd.phoneNumber, cmd.address, cmd.district)
                     )
    } yield customerInfo
}

object UserService {
  val USER_EXISTS = "user.exists"
}
