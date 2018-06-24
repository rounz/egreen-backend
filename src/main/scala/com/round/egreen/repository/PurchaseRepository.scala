// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.repository

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.redis.RedisClientPool
import com.redis.serialization.{Format, Parse}
import com.round.egreen.common.model.ProductSubscription
import com.typesafe.config.Config
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

trait PurchaseRepository[F[_]] {
  def getAllSubscriptions(implicit F: Effect[F]): EitherT[F, String, List[ProductSubscription]]
  def putSubscription(subscription: ProductSubscription)(implicit F: Effect[F]): EitherT[F, String, Unit]
}

object PurchaseRepository {
  val SUBSCRIPTION_NOTFOUND: String    = "subscription.notfound"
  val SUBSCRIPTION_WRITE_ERROR: String = "subscription.write-error"
  val SUBSCRIPTION_READ_ERROR: String  = "subscription.read-error"
}

class RedisPurchaseRepository[F[_]](client: RedisClientPool, config: Config) extends PurchaseRepository[F] {
  import PurchaseRepository._
  import RedisPurchaseRepository._

  private val hash = new Hash(config.getString("redis.hash-suffix"))

  def getAllSubscriptions(implicit F: Effect[F]): EitherT[F, String, List[ProductSubscription]] =
    EitherT(
      F.delay(client.withClient(_.hgetall1[String, ProductSubscription](hash.SUBSCRIPTIONID))).attempt
    ).leftMap(_ => SUBSCRIPTION_READ_ERROR)
      .subflatMap(_.map(_.values.toList).toRight(SUBSCRIPTION_NOTFOUND))

  def putSubscription(subscription: ProductSubscription)(implicit F: Effect[F]): EitherT[F, String, Unit] =
    for {
      _ <- EitherT(
            F.delay(client.withClient(_.hsetnx(hash.SUBSCRIPTIONID, subscription.id, subscription))).attempt
          ).leftMap(_ => SUBSCRIPTION_WRITE_ERROR)
            .ensure(SUBSCRIPTION_WRITE_ERROR)(identity)
    } yield ()
}

object RedisPurchaseRepository {
  implicit val redisFormat: Format = Format {
    case p: ProductSubscription => p.asJson.toString
  }

  implicit val subscriptionParse: Parse[ProductSubscription] =
    Parse(bs => decode[ProductSubscription](new String(bs)).right.get)

  class Hash(suffix: String = "") {
    val SUBSCRIPTIONID: String = s":subscriptionid$suffix"
  }
}
