package com.round

import sbt._

object Dependencies {

  object SbtPlugin {
    val scalafmt: ModuleID           = "com.geirsson"      % "sbt-scalafmt"        % "1.6.0-RC1"
    val partialUnification: ModuleID = "org.lyranthe.sbt"  % "partial-unification" % "1.1.0"
    val kindProjector: ModuleID      = "org.spire-math"    %% "kind-projector"     % "0.9.6"
    val sbtRevolver: ModuleID        = "io.spray"          % "sbt-revolver"        % "0.9.1"
    val sbtHeader: ModuleID          = "de.heikoseeberger" % "sbt-header"          % "5.0.0"
  }

  object Http4s {
    val version: String = "0.18.12"

    val blaze: ModuleID = "org.http4s" %% "http4s-blaze-server" % version
    val circe: ModuleID = "org.http4s" %% "http4s-circe"        % version
    val dsl: ModuleID   = "org.http4s" %% "http4s-dsl"          % version
  }

  object Scalatest {
    val version: String = "3.0.5"

    val core: String = "org.scalatest" %% "scalatest" % version
  }

  object Logback {
    val version: String   = "1.2.3"
    val classic: ModuleID = "ch.qos.logback" % "logback-classic" % version
  }
}
