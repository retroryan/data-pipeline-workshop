name := """data-pipeline-workshop"""

version := "0.43"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"


libraryDependencies ++= Seq(
  ws,
  "org.webjars" % "bootstrap" % "3.1.1",
  "org.webjars" % "jquery" % "2.1.0-2",
  "org.webjars" % "angularjs" % "1.2.16",
  "org.webjars" % "angular-leaflet-directive" % "0.7.6",
  "org.webjars" % "flot" % "0.8.0",
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.4"
)

// Apply digest calculation and gzip compression to assets
pipelineStages := Seq(digest, gzip)

addCommandAlias("rb", "runMain backend.MainTweetLoader 2552 -Dakka.remote.netty.tcp.port=2552 -Dakka.cluster.roles.0=backend-loader")

fork in run := true