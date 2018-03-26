import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.3",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "osm-parsing",
    mainClass in assembly := Some("Main"),
    assemblyMergeStrategy in assembly := {
      case PathList("osmosis-plugins.conf", xs @ _*) => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    },
    libraryDependencies += "org.openstreetmap.osmosis" % "osmosis-pbf" % "0.46",
    libraryDependencies += "com.esri.geometry" % "esri-geometry-api" % "2.1.0",
    libraryDependencies += "joda-time" % "joda-time" % "2.9.9",
    libraryDependencies += "commons-cli" % "commons-cli" % "1.4",
    libraryDependencies += scalaTest % Test
  )
