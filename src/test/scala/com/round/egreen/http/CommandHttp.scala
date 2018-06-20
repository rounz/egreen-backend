// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.http

import java.util.UUID

import cats.data.EitherT
import cats.effect.{Effect, IO}
import com.round.egreen.common.model._
import com.round.egreen.cqrs.command._
import com.round.egreen.repository.UserRepository
import com.round.egreen.service.{UserAuth, UserService}
import com.typesafe.config.{Config, ConfigFactory}
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

  private val userId: UUID    = UUID.randomUUID()
  private val cmdCreateUser   = CreateUser("xxx", "abc", Set(Admin, Customer))
  private val cmdJson: String = cmdCreateUser.asJson.toString

  "CommandHttp object" should {

    "parseCommand correctly" in {
      val parsed: Either[String, CreateUser] = CommandHttp
        .parseCommand[CreateUser, IO](cmdJson)
        .value
        .unsafeRunSync()
      parsed shouldBe Right(cmdCreateUser)
    }

    "parseCommand failed with malformed input" in {
      val parsed: Either[String, CreateUser] = CommandHttp
        .parseCommand[CreateUser, IO]("")
        .value
        .unsafeRunSync()
      parsed shouldBe Left(CommandHttp.COMMAND_NOT_PARSABLE)
    }

    "ensureCommand correctly" in {
      val parsed: Either[String, CreateUser] = CommandHttp
        .ensureCommand[CreateUser, IO](cmdJson, UserClaim(userId, "", Set(Admin), 0, 0))
        .value
        .unsafeRunSync()
      parsed shouldBe Right(cmdCreateUser)
    }

    "ensureCommand correctly with unauthorized sender" in {
      val parsed: Either[String, CreateUser] = CommandHttp
        .ensureCommand[CreateUser, IO](cmdJson, UserClaim(userId, "", Set.empty, 0, 0))
        .value
        .unsafeRunSync()
      parsed shouldBe Left(CommandHttp.PERMISSION_DENIED)
    }
  }

  "CommandHttp service" should {
    val config: Config    = ConfigFactory.parseString("application.secret = abcd")
    val auth: UserAuth    = new UserAuth(config)
    val sender: UserClaim = UserClaim(userId, "asdf", Set(Admin), 0, 0)

    val service: HttpService[IO] = new CommandHttp(auth, new MockUserService(userId)).service

    "parse and process CreateUser command" in {
      val token: String = auth.authToken(User(userId, sender.username, "", sender.roles))
      val response: Response[IO] = service.orNotFound
        .run(createUserRequest(cmdCreateUser.username, token))
        .unsafeRunSync()

      response.status shouldBe Ok
      val Right(user) = parse(new String(response.body.compile.toVector.unsafeRunSync().toArray))
        .flatMap(_.as[User])
      user shouldBe User(userId, cmdCreateUser.username, cmdCreateUser.encryptedPassword, cmdCreateUser.roles)
    }

    "parse and process dev CreateUser with no permission" in {
      val token: String = auth.authToken(User(userId, sender.username, "", Set.empty))
      val response: Response[IO] = service.orNotFound
        .run(createUserRequest("egreen", token))
        .unsafeRunSync()

      response.status shouldBe Ok
      val Right(user) = parse(new String(response.body.compile.toVector.unsafeRunSync().toArray))
        .flatMap(_.as[User])
      user shouldBe User(userId, "egreen", cmdCreateUser.encryptedPassword, cmdCreateUser.roles)
    }
  }

  private def createUserRequest(username: String, token: String): Request[IO] =
    Request[IO](
      method  = Method.POST,
      headers = Headers(Authorization(Credentials.Token(AuthScheme.Bearer, token)) :: Nil),
      uri     = Uri.uri("/")
    ).withBody(cmdCreateUser.copy(username = username).envelope.asJson)
      .unsafeRunSync()
}

class MockUserService(userId: UUID) extends UserService[IO](null, null) {
  override def checkUserExists(username: String)(implicit F: Effect[IO]): EitherT[IO, String, Boolean] =
    EitherT.rightT(false)

  override def getUser(username: String)(implicit F: Effect[IO]): EitherT[IO, String, User] =
    EitherT.leftT(UserRepository.USER_NOTFOUND)

  override def createUser(cmd: CreateUser)(implicit F: Effect[IO]): EitherT[IO, String, User] =
    EitherT.rightT(User(userId, cmd.username, cmd.encryptedPassword, cmd.roles))
}
