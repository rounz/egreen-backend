// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.repository

import java.util.UUID

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.redis.RedisClientPool
import com.redis.serialization.{Format, Parse}
import com.round.egreen.common.model.ProductPackage
import com.typesafe.config.Config
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

trait ProductRepository[F[_]] {
  def getAllPackages(implicit F: Effect[F]): EitherT[F, String, List[ProductPackage]]
  def getPackage(productId: UUID)(implicit F: Effect[F]): EitherT[F, String, ProductPackage]
  def putPackage(product: ProductPackage)(implicit F: Effect[F]): EitherT[F, String, Unit]
  def updatePackage(product: ProductPackage)(implicit F: Effect[F]): EitherT[F, String, Unit]
}

object ProductRepository {
  val PRODUCT_NOTFOUND: String    = "product-package.notfound"
  val PRODUCT_WRITE_ERROR: String = "product-package.write-error"
  val PRODUCT_READ_ERROR: String  = "product-package.read-error"
}

class RedisProductRepository[F[_]](client: RedisClientPool, config: Config) extends ProductRepository[F] {
  import ProductRepository._
  import RedisProductRepository._

  private val hash = new Hash(config.getString("redis.hash-suffix"))

  def getAllPackages(implicit F: Effect[F]): EitherT[F, String, List[ProductPackage]] =
    EitherT(
      F.delay(client.withClient(_.hgetall1[String, ProductPackage](hash.PRODUCTID))).attempt
    ).leftMap(_ => PRODUCT_READ_ERROR)
      .subflatMap(_.map(_.values.toList).toRight(PRODUCT_NOTFOUND))

  def getPackage(productId: UUID)(implicit F: Effect[F]): EitherT[F, String, ProductPackage] =
    for {
      product <- EitherT(
                  F.delay(client.withClient(_.hget[ProductPackage](hash.PRODUCTID, productId))).attempt
                ).leftMap(_ => PRODUCT_READ_ERROR)
                  .subflatMap(_.toRight(PRODUCT_NOTFOUND))
    } yield product

  def putPackage(product: ProductPackage)(implicit F: Effect[F]): EitherT[F, String, Unit] =
    for {
      _ <- EitherT(
            F.delay(client.withClient(_.hsetnx(hash.PRODUCTID, product.id, product))).attempt
          ).leftMap(_ => PRODUCT_WRITE_ERROR)
            .ensure(PRODUCT_WRITE_ERROR)(identity)
    } yield ()

  def updatePackage(product: ProductPackage)(implicit F: Effect[F]): EitherT[F, String, Unit] =
    for {
      _ <- EitherT(
            F.delay(client.withClient(_.hexists(hash.PRODUCTID, product.id))).attempt
          ).leftMap(_ => PRODUCT_READ_ERROR)
            .ensure(PRODUCT_NOTFOUND)(identity)
      _ <- EitherT(
            F.delay(client.withClient(_.hset1(hash.PRODUCTID, product.id, product))).attempt
          ).leftMap(_ => PRODUCT_WRITE_ERROR)
            .ensure(PRODUCT_WRITE_ERROR)(_ == 0.some)
    } yield ()
}

object RedisProductRepository {
  implicit val redisFormat: Format = Format {
    case p: ProductPackage => p.asJson.toString
  }

  implicit val productParse: Parse[ProductPackage] =
    Parse(bs => decode[ProductPackage](new String(bs)).right.get)

  class Hash(suffix: String = "") {
    val PRODUCTID: String = s":productid$suffix"
  }
}
