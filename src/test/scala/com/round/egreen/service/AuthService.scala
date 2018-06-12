// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.service

import java.util.UUID

import cats.effect.IO
import com.round.egreen.common.model._
import com.typesafe.config.{Config, ConfigFactory}
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.scalatest.{Matchers, WordSpec}

class UserAuthSpec extends WordSpec with Matchers with Http4sDsl[IO] {
  import UserAuth.UserClaim

  private val config: Config    = ConfigFactory.parseString("application.secret = abcd")
  private val auth: UserAuth    = new UserAuth(config)
  private val userId: UUID      = UUID.randomUUID()
  private val sender: UserClaim = UserClaim(userId, "asdf", Set(Admin), 0, 0)
  private val token: String     = auth.authToken(User(userId, sender.username, "", sender.roles))

  private val service: HttpService[IO] = auth {
    AuthedService[UserClaim, IO] {
      case GET -> Root as user => Ok(user.username)
    }
  }

  "UserAuth-ed service" should {
    "authorize requests with correct token" in {
      val response: Response[IO] = request(token)
      response.status shouldBe Ok
    }

    "not authorize requests with incorrect token" in {
      val response: Response[IO] = request(token.drop(2))
      response.status shouldBe Unauthorized
    }
  }

  private def request(token: String): Response[IO] =
    service.orNotFound
      .run(
        Request(
          method  = Method.GET,
          headers = Headers(Authorization(Credentials.Token(AuthScheme.Bearer, token)) :: Nil),
          uri     = Uri.uri("/")
        )
      )
      .unsafeRunSync()
}
