name := "osm-jigsaw-viewer"
version := "1.0"

lazy val `osm-jigsaw-viewer` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(ws)
libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.12"
libraryDependencies += guice

libraryDependencies += specs2 % Test

enablePlugins(DockerPlugin)
dockerBaseImage := "openjdk:8-jre"
dockerExposedPorts in Docker := Seq(9000)