import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "uk.co.eelpieconsulting",
      scalaVersion := "2.12.3",
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
    libraryDependencies += "org.openstreetmap.osmosis" % "osmosis-pbf" % "0.46",
    libraryDependencies += "com.esri.geometry" % "esri-geometry-api" % "2.1.0",
    libraryDependencies += "joda-time" % "joda-time" % "2.9.9",
    libraryDependencies += "commons-cli" % "commons-cli" % "1.4",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.11.0",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.11.0",
    libraryDependencies += "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0",
    libraryDependencies += "org.mapdb" % "mapdb" % "3.0.5",
    libraryDependencies += scalaTest % Test
  )
