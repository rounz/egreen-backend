// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.http

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.round.egreen.common.model.Error
import com.round.egreen.cqrs.command._
import com.round.egreen.service._
import io.circe.{Decoder, Json}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class CommandHttp[F[_]: Effect](userAuth: UserAuth,
                                userService: UserService[F],
                                productService: ProductService[F],
                                purchaseService: PurchaseService[F])
    extends Http4sDsl[F] {
  import CommandHttp._
  import UserAuth.UserClaim

  val service: HttpService[F] = userAuth {
    AuthedService[UserClaim, F] {
      case (request @ POST -> Root) as sender =>
        for {
          cmd <- request.as[CommandEnvelope]

          json <- (if (cmd.commandName == CreateUser.commandName) {
                     for {
                       userCmd <- parseCommand[CreateUser, F](cmd.json)
                                   .ensure(PERMISSION_DENIED) {
                                     _.username == "egreen" || CreateUser.permission.isAllowed(sender)
                                   }
                       user <- userService.createUser(userCmd)
                     } yield user.asJson

                   } else if (cmd.commandName == GetAllUsers.commandName) {
                     for {
                       _     <- ensureCommand[GetAllUsers, F](cmd.json, sender)
                       users <- userService.getAllUsers
                     } yield users.asJson

                   } else if (cmd.commandName == CreateCustomer.commandName) {
                     for {
                       userCmd  <- ensureCommand[CreateCustomer, F](cmd.json, sender)
                       customer <- userService.createCustomer(userCmd)
                     } yield customer.asJson

                   } else if (cmd.commandName == GetCustomer.commandName) {
                     for {
                       userCmd  <- ensureCommand[GetCustomer, F](cmd.json, sender)
                       customer <- userService.getCustomer(userCmd)
                     } yield customer.asJson

                   } else if (cmd.commandName == UpdateCustomer.commandName) {
                     for {
                       userCmd  <- ensureCommand[UpdateCustomer, F](cmd.json, sender)
                       customer <- userService.updateCustomer(userCmd)
                     } yield customer.asJson

                   } else if (cmd.commandName == GetAllCustomers.commandName) {
                     for {
                       _  <- ensureCommand[GetAllCustomers, F](cmd.json, sender)
                       cs <- userService.getAllCustomers
                     } yield cs.asJson

                   } else if (cmd.commandName == CreateProductPackage.commandName) {
                     for {
                       pCmd    <- ensureCommand[CreateProductPackage, F](cmd.json, sender)
                       product <- productService.createPackage(pCmd)
                     } yield product.asJson

                   } else if (cmd.commandName == UpdateProductPackage.commandName) {
                     for {
                       pCmd    <- ensureCommand[UpdateProductPackage, F](cmd.json, sender)
                       product <- productService.updatePackage(pCmd)
                     } yield product.asJson

                   } else if (cmd.commandName == GetAllProductPackages.commandName) {
                     for {
                       _  <- ensureCommand[GetAllProductPackages, F](cmd.json, sender)
                       ps <- productService.getAllPackages
                     } yield ps.asJson

                   } else if (cmd.commandName == CreateProductSubscription.commandName) {
                     for {
                       pCmd <- ensureCommand[CreateProductSubscription, F](cmd.json, sender)
                       subs <- purchaseService.createSubscription(pCmd)
                     } yield subs.asJson

                   } else if (cmd.commandName == GetAllProductSubscriptions.commandName) {
                     for {
                       _    <- ensureCommand[GetAllProductSubscriptions, F](cmd.json, sender)
                       subs <- purchaseService.getAllSubscriptions
                     } yield subs.asJson

                   } else {
                     EitherT.leftT[F, Json](COMMAND_NOT_SUPPORTED)
                   }).value

          response <- json.fold(
                       e => BadRequest(Error(e).asJson),
                       Ok(_)
                     )
        } yield response
    }
  }
}

object CommandHttp {
  import UserAuth.UserClaim

  val COMMAND_NOT_SUPPORTED = "command.not-supported"
  val COMMAND_NOT_PARSABLE  = "command.not-parsable"
  val PERMISSION_DENIED     = "permission.denied"

  def ensureCommand[C, F[_]](json: String, sender: UserClaim)(implicit
                                                              D: Decoder[C],
                                                              P: Permission[C],
                                                              F: Effect[F]): EitherT[F, String, C] =
    parseCommand[C, F](json)
      .ensure(PERMISSION_DENIED)(_ => P.isAllowed(sender))

  def parseCommand[C: Decoder, F[_]: Effect](json: String): EitherT[F, String, C] =
    EitherT.fromEither[F](
      parse(json).flatMap(_.as[C]).leftMap(_ => COMMAND_NOT_PARSABLE)
    )
}
