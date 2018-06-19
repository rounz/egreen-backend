// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.repository

import cats.data.EitherT
import cats.effect.Effect
import com.redis.RedisClientPool
import com.redis.serialization.{Format, Parse}
import com.round.egreen.common.model.User
import com.typesafe.config.Config
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

trait UserRepository[F[_]] {
  def checkUserExists(username: String)(implicit F: Effect[F]): F[Boolean]
  def getUser(username: String)(implicit F: Effect[F]): EitherT[F, String, User]
  def putUser(user: User)(implicit F: Effect[F]): EitherT[F, String, Unit]
}

object UserRepository {
  val USER_NOTFOUND    = "user.notfound"
  val USER_WRITE_ERROR = "user.write-error"
}

class RedisUserRepository[F[_]](client: RedisClientPool, config: Config) extends UserRepository[F] {
  import UserRepository._
  import RedisUserRepository._

  private val hash = new Hash(config.getString("redis.hash-suffix"))

  def checkUserExists(username: String)(implicit F: Effect[F]): F[Boolean] =
    F.delay(
      client.withClient(_.hexists(hash.USERNAME, username))
    )

  def getUser(username: String)(implicit F: Effect[F]): EitherT[F, String, User] =
    EitherT(
      F.delay(
        client.withClient(_.hget[User](hash.USERNAME, username)).toRight(USER_NOTFOUND)
      )
    )

  def putUser(user: User)(implicit F: Effect[F]): EitherT[F, String, Unit] =
    for {
      _ <- EitherT
            .right(F.delay(client.withClient(_.hsetnx(hash.USERID, user.id, user))))
            .ensure(USER_WRITE_ERROR)(identity)
      _ <- EitherT
            .right(F.delay(client.withClient(_.hsetnx(hash.USERNAME, user.username, user))))
            .ensure(USER_WRITE_ERROR)(identity)
    } yield ()
}

object RedisUserRepository {
  implicit val userFormat: Format =
    Format { case user: User => user.asJson.toString }

  implicit val userParse: Parse[User] =
    Parse(bs => decode[User](new String(bs)).right.get)

  class Hash(suffix: String = "") {
    val USERID: String   = s":userid$suffix"
    val USERNAME: String = s":username$suffix"
  }
}
