name := "osm-jigsaw-viewer"
version := "1.0"

PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)
PB.protoSources in Compile := Seq(sourceDirectory.value / "protobuf")

lazy val `osm-jigsaw-viewer` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies += "com.esri.geometry" % "esri-geometry-api" % "2.1.0"
libraryDependencies += specs2 % Test

enablePlugins(DockerPlugin)
dockerBaseImage := "openjdk:8-jre"
dockerExposedPorts in Docker := Seq(9000)

javaOptions in Universal ++= Seq(
  // -J params will be added as jvm parameters
  "-J-XX:+UnlockExperimentalVMOptions", "-J-XX:+UseCGroupMemoryLimitForHeap", "-J-XX:MaxRAMFraction=1", "-J-XshowSettings:vm", "-J-Xmx30G"
)
