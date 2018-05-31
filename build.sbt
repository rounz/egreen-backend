import com.round.Dependencies._

lazy val root = (project in file("."))
  .settings(
    organization := "com.round",
    name         := "egreen-backend",
    version      := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      Http4s.blaze,
      Http4s.circe,
      Http4s.dsl,
      Logback.classic
    )
  )

addCommandAlias(
  "fmt",
  ";scalafmtSbt;scalafmt;test:scalafmt"
)

// while you're working, try putting "~wip" into your sbt console
// ...but be prepared to let IntelliJ force you to reload!
addCommandAlias("wip", ";fmt;test:compile")
