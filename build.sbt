name := """play-node-benchmark"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe" %% "jse" % "1.1.2"
)

parallelExecution in Test := false

fork in test := true