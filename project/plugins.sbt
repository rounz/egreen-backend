import com.round.Dependencies.SbtPlugin._

addSbtPlugin(scalafmt)
addSbtPlugin(scalafix)
addSbtPlugin(partialUnification)
addSbtPlugin(sbtRevolver)
addSbtPlugin(coursier)
addSbtPlugin(sbtHeader)
addSbtPlugin(buildInfo)
addSbtPlugin(packager)
addSbtPlugin(protoc)

libraryDependencies += com.round.Dependencies.ScalaPB.compiler
