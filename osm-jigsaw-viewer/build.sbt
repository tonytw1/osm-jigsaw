name := "osm-jigsaw-viewer"
version := "1.0"

lazy val `osm-jigsaw-viewer` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.13"

libraryDependencies ++= Seq(ws)
libraryDependencies += "io.lemonlabs" %% "scala-uri" % "3.6.0"
libraryDependencies += guice

libraryDependencies += specs2 % Test

enablePlugins(DockerPlugin)
dockerBaseImage := "openjdk:11-jre"
dockerExposedPorts in Docker := Seq(9000)