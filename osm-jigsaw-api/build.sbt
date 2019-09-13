name := "osm-jigsaw-api"
version := "1.0"

PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)
PB.protoSources in Compile := Seq(sourceDirectory.value / "protobuf")

lazy val `osm-jigsaw-viewer` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.9"

libraryDependencies += guice
libraryDependencies += "com.esri.geometry" % "esri-geometry-api" % "2.1.0"
libraryDependencies += "com.google.guava" % "guava" % "25.1-jre"
libraryDependencies += specs2 % Test
libraryDependencies += "org.mockito" % "mockito-core" % "1.9.5" % Test

enablePlugins(DockerPlugin)
dockerBaseImage := "openjdk:10-jre"
dockerExposedPorts in Docker := Seq(9000)

javaOptions in Universal ++= Seq(
  // -J params will be added as jvm parameters
  "-J-XshowSettings:vm", "-J-XX:+PrintCommandLineFlags", "-J-XX:+UseConcMarkSweepGC", "-J-XX:MaxRAMPercentage=100"
)
