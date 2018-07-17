// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.repository

import java.util.UUID

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.redis.RedisClientPool
import com.redis.serialization.{Format, Parse}
import com.round.egreen.common.model.{CustomerInfo, User}
import com.typesafe.config.Config
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

trait UserRepository[F[_]] {
  def checkUserExists(username: String)(implicit F: Effect[F]): EitherT[F, String, Boolean]
  def getUser(username: String)(implicit F: Effect[F]): EitherT[F, String, User]
  def getUser(userId: UUID)(implicit F: Effect[F]): EitherT[F, String, User]
  def getAllUsers(implicit F: Effect[F]): EitherT[F, String, List[User]]
  def putUser(user: User)(implicit F: Effect[F]): EitherT[F, String, Unit]
  def getCustomerInfo(userId: UUID)(implicit F: Effect[F]): EitherT[F, String, CustomerInfo]
  def putCustomerInfo(customerInfo: CustomerInfo)(implicit F: Effect[F]): EitherT[F, String, Unit]
  def getAllCustomers(implicit F: Effect[F]): EitherT[F, String, List[CustomerInfo]]
}

object UserRepository {
  val USER_NOTFOUND: String     = "user.notfound"
  val USER_WRITE_ERROR: String  = "user.write-error"
  val USER_READ_ERROR: String   = "user.read-error"
  val CUSTOMER_NOTFOUND: String = "customer.notfound"
}

class RedisUserRepository[F[_]](client: RedisClientPool, config: Config) extends UserRepository[F] {
  import UserRepository._
  import RedisUserRepository._

  private val hash = new Hash(config.getString("redis.hash-suffix"))

  def checkUserExists(username: String)(implicit F: Effect[F]): EitherT[F, String, Boolean] =
    EitherT(
      F.delay(
          client.withClient(_.hexists(hash.USERNAME, username))
        )
        .attempt
    ).leftMap(_ => USER_READ_ERROR)

  def getUser(username: String)(implicit F: Effect[F]): EitherT[F, String, User] =
    EitherT(
      F.delay(client.withClient(_.hget[User](hash.USERNAME, username))).attempt
    ).leftMap(_ => USER_READ_ERROR)
      .subflatMap(_.toRight(USER_NOTFOUND))

  def getUser(userId: UUID)(implicit F: Effect[F]): EitherT[F, String, User] =
    EitherT(
      F.delay(client.withClient(_.hget[User](hash.USERID, userId))).attempt
    ).leftMap(_ => USER_READ_ERROR)
      .subflatMap(_.toRight(USER_NOTFOUND))

  def getAllUsers(implicit F: Effect[F]): EitherT[F, String, List[User]] =
    EitherT(
      F.delay(client.withClient(_.hgetall1[String, User](hash.USERID))).attempt
    ).leftMap(_ => USER_READ_ERROR)
      .subflatMap(_.map(_.values.toList).toRight(USER_NOTFOUND))

  def putUser(user: User)(implicit F: Effect[F]): EitherT[F, String, Unit] =
    for {
      _ <- EitherT(
            F.delay(client.withClient(_.hsetnx(hash.USERID, user.id, user))).attempt
          ).leftMap(_ => USER_WRITE_ERROR)
            .ensure(USER_WRITE_ERROR)(identity)
      _ <- EitherT(
            F.delay(client.withClient(_.hsetnx(hash.USERNAME, user.username, user))).attempt
          ).leftMap(_ => USER_WRITE_ERROR)
            .ensure(USER_WRITE_ERROR)(identity)
    } yield ()

  def getCustomerInfo(userId: UUID)(implicit F: Effect[F]): EitherT[F, String, CustomerInfo] =
    EitherT(
      F.delay(
          client.withClient(_.hget[CustomerInfo](hash.CUSTOMER_INFO, userId))
        )
        .attempt
    ).leftMap(_ => USER_WRITE_ERROR)
      .subflatMap(_.toRight(USER_NOTFOUND))

  def putCustomerInfo(customerInfo: CustomerInfo)(implicit F: Effect[F]): EitherT[F, String, Unit] =
    EitherT(
      F.delay(
          client.withClient(_.hset1(hash.CUSTOMER_INFO, customerInfo.userId, customerInfo))
        )
        .attempt
    ).leftMap(_ => USER_WRITE_ERROR)
      .map(_ => ())

  def getAllCustomers(implicit F: Effect[F]): EitherT[F, String, List[CustomerInfo]] =
    EitherT(
      F.delay(client.withClient(_.hgetall1[String, CustomerInfo](hash.CUSTOMER_INFO))).attempt
    ).leftMap(_ => USER_READ_ERROR)
      .subflatMap(_.map(_.values.toList).toRight(CUSTOMER_NOTFOUND))
}

object RedisUserRepository {
  implicit val redisFormat: Format = Format {
    case user: User             => user.asJson.toString
    case customer: CustomerInfo => customer.asJson.toString
  }

  implicit val userParse: Parse[User] =
    Parse(bs => decode[User](new String(bs)).right.get)

  implicit val customerParse: Parse[CustomerInfo] =
    Parse(bs => decode[CustomerInfo](new String(bs)).right.get)

  class Hash(suffix: String = "") {
    val USERID: String        = s":userid$suffix"
    val USERNAME: String      = s":username$suffix"
    val CUSTOMER_INFO: String = s":customer$suffix"
  }
}
