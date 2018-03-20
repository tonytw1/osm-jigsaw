import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.3",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    libraryDependencies += "org.openstreetmap.osmosis" % "osmosis-pbf" % "0.46",
    libraryDependencies += "joda-time" % "joda-time" % "2.9.9",
    libraryDependencies += scalaTest % Test
  )
