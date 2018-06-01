package com.round.egreen

import cats.effect.IO
import com.round.egreen.build.BuildInfo
import com.round.egreen.http.CoreHttp
import com.round.egreen.service.CoreService
import org.http4s._
import org.http4s.implicits._
import org.scalatest._

class CoreHttpSpec extends WordSpec with Matchers {

  "/health endpoint" should {
    "return 200" in {
      healthCheck.status shouldBe Status.Ok
    }

    "return status OK" in {
      healthCheck.as[String].unsafeRunSync() shouldBe """{"status":"OK"}"""
    }
  }

  "/version endpoint" should {
    "return 200" in {
      buildInfo.status shouldBe Status.Ok
    }

    "return build info" in {
      buildInfo.as[String].unsafeRunSync().replaceAll("\\s", "") shouldBe BuildInfo.toJson.replaceAll("\\s", "")
    }
  }

  private[this] val coreHttp: HttpService[IO] = new CoreHttp[IO](new CoreService).service

  private[this] val healthCheck: Response[IO] = {
    val getHW = Request[IO](Method.GET, Uri.uri("/health"))
    coreHttp.orNotFound(getHW).unsafeRunSync()
  }

  private[this] val buildInfo: Response[IO] = {
    val getHW = Request[IO](Method.GET, Uri.uri("/version"))
    coreHttp.orNotFound(getHW).unsafeRunSync()
  }
}
