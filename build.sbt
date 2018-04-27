
name := "osm-jigsaw-viewer"
version := "1.0"

PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)
PB.protoSources in Compile := Seq(sourceDirectory.value / "protobuf")

lazy val `osm-jigsaw-viewer` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies += specs2 % Test
