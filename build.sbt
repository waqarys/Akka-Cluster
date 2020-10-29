name := "loyalty"

organization := "reactivebbq"

version := "0.1-SNAPSHOT"

scalaVersion := "2.13.1"

lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion    = "2.6.0"
lazy val akkaManagementVersion =  "1.0.5"
lazy val akkaExtensionsVersion = "1.1.12"
lazy val logbackVersion = "1.2.3"
lazy val scalaTestVersion = "3.1.0"

fork := true
parallelExecution in ThisBuild := false

scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-cluster"         % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding"% akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
  "com.lightbend.akka" %% "akka-diagnostics" % akkaExtensionsVersion,
  "com.lightbend.akka" %% "akka-split-brain-resolver" % akkaExtensionsVersion,

  //Logback
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,

  //Test dependencies
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % scalaTestVersion% Test,

  Cinnamon.library.cinnamonAkka,
  Cinnamon.library.cinnamonAkkaHttp,
  Cinnamon.library.cinnamonJvmMetricsProducer,
  Cinnamon.library.cinnamonPrometheus,
  Cinnamon.library.cinnamonPrometheusHttpServer
)

enablePlugins(Cinnamon)

cinnamon in run := true
cinnamon in test := true
