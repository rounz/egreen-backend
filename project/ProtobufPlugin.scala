package com.round

import sbt._
import sbt.Keys._
import sbtprotoc.ProtocPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB

/**
  * Common project settings.
  */
object ProtobufPlugin extends AutoPlugin {

  override def requires: Plugins = ProtocPlugin
  override def trigger           = allRequirements

  override val projectSettings: Seq[Def.Setting[_]] = Seq(
    watchSources ++= (sourceDirectory.in(Compile).value / "protobuf" ** "*.proto").get,
    unmanagedResourceDirectories in Compile += sourceDirectory.in(Compile).value / "protobuf",
    unmanagedSourceDirectories in Compile += sourceDirectory.in(Compile).value / "protobuf",
    PB.targets in Compile := Seq(
      scalapb.gen(flatPackage = true) -> sourceManaged.in(Compile).value / "protobuf"
    )
  )
}
