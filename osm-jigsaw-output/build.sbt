name := "osm-jigsaw-output"
version := "1.0"

import Dependencies._

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "uk.co.eelpieconsulting",
      scalaVersion := "2.12.10",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "osm-jigsaw",
    mainClass in assembly := Some("Main"),
    assemblyMergeStrategy in assembly := {
      case PathList("osmosis-plugins.conf", xs @ _*) => MergeStrategy.discard
      case "META-INF/MANIFEST.MF" => MergeStrategy.discard
      case "META-INF/ECLIPSE_.RSA" => MergeStrategy.discard
      case "META-INF/ECLIPSE_.SF" => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    libraryDependencies += "commons-cli" % "commons-cli" % "1.4",
    libraryDependencies += "com.google.guava" % "guava" % "23.0",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.11.0",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.11.0",
    libraryDependencies += "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0",
    libraryDependencies += scalaTest % Test
  )

mainClass in Compile := Some("Main")
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
dockerBaseImage := "openjdk:10-jre"

