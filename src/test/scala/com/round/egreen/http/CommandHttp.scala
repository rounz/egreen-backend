// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.http

import cats.data.EitherT
import cats.effect.{Effect, IO}
import com.round.egreen.common.model._
import com.round.egreen.cqrs.command
import com.round.egreen.repository.UserRepository
import com.round.egreen.service.{UserAuth, UserService}
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.scalatest.{Matchers, WordSpec}

class CommandHttpSpec extends WordSpec with Matchers with Http4sDsl[IO] {
  import UserAuth.UserClaim

  private val cmdCreateUser   = command.CreateUser("xxx", "abc", Set(Admin, Customer))
  private val cmdJson: String = cmdCreateUser.asJson.toString

  "CommandHttp object" should {

    "parseCommand correctly" in {
      val parsed: Either[String, command.CreateUser] = CommandHttp
        .parseCommand[command.CreateUser, IO](cmdJson)
        .value
        .unsafeRunSync()
      parsed shouldBe Right(cmdCreateUser)
    }

    "parseCommand failed with malformed input" in {
      val parsed: Either[String, command.CreateUser] = CommandHttp
        .parseCommand[command.CreateUser, IO]("")
        .value
        .unsafeRunSync()
      parsed shouldBe Left(CommandHttp.COMMAND_NOT_PARSABLE)
    }

    "ensureCommand correctly" in {
      val parsed: Either[String, command.CreateUser] = CommandHttp
        .ensureCommand[command.CreateUser, IO](cmdJson, UserClaim("", Set(Admin), 0, 0))
        .value
        .unsafeRunSync()
      parsed shouldBe Right(cmdCreateUser)
    }

    "ensureCommand correctly with unauthorized sender" in {
      val parsed: Either[String, command.CreateUser] = CommandHttp
        .ensureCommand[command.CreateUser, IO](cmdJson, UserClaim("", Set.empty, 0, 0))
        .value
        .unsafeRunSync()
      parsed shouldBe Left(CommandHttp.PERMISSION_DENIED)
    }
  }

  "CommandHttp object" should {
    val config: Config    = ConfigFactory.parseString("application.secret = abcd")
    val auth: UserAuth    = new UserAuth(config)
    val sender: UserClaim = UserClaim("asdf", Set(Admin), 0, 0)
    val token: String     = auth.authToken(User(sender.username, "", sender.roles))

    val service: HttpService[IO] = new CommandHttp(auth, MockUserService).service

    "parse and process CreateUser command" in {
      val response: Response[IO] = service.orNotFound
        .run(
          Request[IO](
            method  = Method.POST,
            headers = Headers(Authorization(Credentials.Token(AuthScheme.Bearer, token)) :: Nil),
            uri     = Uri.uri("/")
          ).withBody(command.CommandEnvelope(command.CreateUser.commandName, cmdJson).asJson)
            .unsafeRunSync()
        )
        .unsafeRunSync()

      response.status shouldBe Ok
      val Right(json) = parse(new String(response.body.compile.toVector.unsafeRunSync().toArray))
      val Right(user) = json.as[User]
      user shouldBe User(cmdCreateUser.username, cmdCreateUser.encryptedPassword, cmdCreateUser.roles)
    }
  }
}

object MockUserService extends UserService[IO](null, null) {
  override def checkUserExists(username: String)(implicit F: Effect[IO]): IO[Boolean] =
    IO(false)

  override def getUser(username: String)(implicit F: Effect[IO]): EitherT[IO, String, User] =
    EitherT.leftT(UserRepository.USER_NOTFOUND)

  override def createUser(cmd: command.CreateUser)(implicit F: Effect[IO]): EitherT[IO, String, Json] =
    EitherT.rightT(User(cmd.username, cmd.encryptedPassword, cmd.roles).asJson)
}
