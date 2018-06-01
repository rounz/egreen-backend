import com.round.Dependencies.SbtPlugin._

addCompilerPlugin(kindProjector)

addSbtPlugin(scalafmt)
addSbtPlugin(scalafix)
addSbtPlugin(partialUnification)
addSbtPlugin(sbtRevolver)
addSbtPlugin(coursier)
addSbtPlugin(sbtHeader)
addSbtPlugin(buildInfo)
addSbtPlugin(scoverage)
