name := "osm-jigsaw-viewer"
version := "1.0"

lazy val `osm-jigsaw-viewer` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.13"

libraryDependencies ++= Seq(ws, guice)
libraryDependencies += "io.lemonlabs" %% "scala-uri" % "1.5.1"

libraryDependencies += specs2 % Test

enablePlugins(DockerPlugin)
dockerBaseImage := "openjdk:11-jre"
dockerExposedPorts in Docker := Seq(9000)