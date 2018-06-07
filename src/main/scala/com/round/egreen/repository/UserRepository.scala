// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.repository

import cats.data.EitherT
import cats.effect.Effect
import com.redis.RedisClientPool
import com.redis.serialization.{Format, Parse}
import com.round.egreen.common.model.User
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

trait UserRepository[F[_]] {
  def getUser(username: String)(implicit F: Effect[F]): EitherT[F, String, User]
  def putUser(user: User)(implicit F: Effect[F]): EitherT[F, String, Unit]
}

object UserRepository {
  val USER_NOTFOUND    = "user.notfound"
  val USER_WRITE_ERROR = "user.write-error"
}

class RedisUserRepository[F[_]](client: RedisClientPool) extends UserRepository[F] {
  import UserRepository._
  import RedisUserRepository._

  def getUser(username: String)(implicit F: Effect[F]): EitherT[F, String, User] =
    EitherT(
      F.delay(
        client.withClient(_.get[User](username)).toRight(USER_NOTFOUND)
      )
    )

  def putUser(user: User)(implicit F: Effect[F]): EitherT[F, String, Unit] =
    EitherT
      .right(F.delay(client.withClient(_.set(user.username, user))))
      .ensure(USER_WRITE_ERROR)(identity)
      .map(_ => ())
}

object RedisUserRepository {
  implicit val userFormat: Format =
    Format { case user: User => user.asJson.toString }

  implicit val userParse: Parse[User] =
    Parse(bs => decode[User](new String(bs)).right.get)
}
