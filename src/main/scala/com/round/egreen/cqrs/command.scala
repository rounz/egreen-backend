// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.cqrs.command

import cats.effect.Effect
import com.round.egreen.common.model._
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe._

final case class CommandEnvelope(commandName: String, json: String)

object CommandEnvelope {
  implicit def decoder[F[_]: Effect]: EntityDecoder[F, CommandEnvelope] =
    jsonOf[F, CommandEnvelope]
}

sealed trait Command

final case class Permission[T](authorizedRoles: Set[Role]) {

  def isAllowed(user: User): Boolean =
    (authorizedRoles + Developer).exists(user.roles.contains)
}

final case class CreateUser(
    username: String,
    encryptedPassword: String,
    roles: List[Role]
) extends Command

object CreateUser {
  val commandName: String = classOf[CreateUser].getName

  implicit val permission: Permission[CreateUser] =
    Permission[CreateUser](Set(Admin))
}

final case class UserLogin(
    username: String,
    encryptedPassword: String
) extends Command

object UserLogin {
  implicit def decoder[F[_]: Effect]: EntityDecoder[F, UserLogin] =
    jsonOf[F, UserLogin]
}
