// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.cqrs.command

import java.util.UUID

import cats.effect.Effect
import com.round.egreen.common.model._
import com.round.egreen.service.UserAuth
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.EntityDecoder
import org.http4s.circe._

import scala.reflect.ClassTag

final case class CommandEnvelope(commandName: String, json: String)

object CommandEnvelope {
  implicit def decoder[F[_]: Effect]: EntityDecoder[F, CommandEnvelope] =
    jsonOf[F, CommandEnvelope]
}

sealed trait Command[C <: Command[C]] { self: C =>

  def envelope(implicit C: ClassTag[C], E: Encoder[C]): CommandEnvelope =
    CommandEnvelope(C.runtimeClass.getName, self.asJson.toString)
}

sealed trait CommandCompanion[C <: Command[C]] {
  def commandName(implicit C: ClassTag[C]): String = C.runtimeClass.getName
}

final case class Permission[T](authorizedRoles: Set[Role]) {

  def isAllowed(user: UserAuth.UserClaim): Boolean =
    (authorizedRoles + Developer).exists(user.roles.contains)
}

final case class CreateUser(
    username: String,
    encryptedPassword: String,
    roles: Set[Role]
) extends Command[CreateUser]

object CreateUser extends CommandCompanion[CreateUser] {

  implicit val permission: Permission[CreateUser] =
    Permission[CreateUser](Set(Admin))
}

final case class GetAllUsers(
    )
    extends Command[GetAllUsers]

object GetAllUsers extends CommandCompanion[GetAllUsers] {
  implicit val permission: Permission[GetAllUsers] =
    Permission[GetAllUsers](Set(Admin))
}

final case class CreateCustomer(
    username: String,
    encryptedPassword: String,
    fullName: String,
    phoneNumber: String,
    address: String,
    district: District
) extends Command[CreateCustomer]

object CreateCustomer extends CommandCompanion[CreateCustomer] {
  implicit val permission: Permission[CreateCustomer] =
    Permission[CreateCustomer](Set(Admin))
}

final case class UpdateCustomer(
    id: UUID,
    username: String,
    encryptedPassword: String,
    fullName: String,
    phoneNumber: String,
    address: String,
    district: District
) extends Command[UpdateCustomer]

object UpdateCustomer extends CommandCompanion[UpdateCustomer] {
  implicit val permission: Permission[UpdateCustomer] =
    Permission[UpdateCustomer](Set(Admin))
}

final case class GetCustomer(
    userId: UUID
) extends Command[GetCustomer]

object GetCustomer extends CommandCompanion[GetCustomer] {
  implicit val permission: Permission[GetCustomer] =
    Permission[GetCustomer](Set(Admin))
}

final case class UserLogin(
    username: String,
    encryptedPassword: String
) extends Command[UserLogin]

object UserLogin extends CommandCompanion[UserLogin] {
  implicit def decoder[F[_]: Effect]: EntityDecoder[F, UserLogin] =
    jsonOf[F, UserLogin]
}
