name := "botsstackoverflow"

version := "0.1"

scalaVersion := "2.12.4"

organization := "in.internity"

// The necessary dependencies can be added here

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-http-xml" % "10.0.10",
  "org.twitter4j" % "twitter4j-core" % "4.0.6",
  //For json Conversion
  "org.json4s" %% "json4s-native" % "3.5.3",
  "de.heikoseeberger" %% "akka-http-json4s" % "1.11.0",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  "org.apache.commons" % "commons-dbcp2" % "2.0.1",
  "net.ruippeixotog" %% "scala-scraper" % "2.0.0",
  "com.flyberrycapital" %% "scala-slack" % "0.3.1",
  "org.scalatest" %% "scalatest" % "3.0.0" % Test,
  "org.mockito" % "mockito-core" % "2.8.47" % Test
)

dependencyOverrides ++= Seq(
  "org.json4s" %% "json4s-ast" % "3.5.3",
  "org.json4s" %% "json4s-core" % "3.5.3",
  "commons-net" % "commons-net" % "2.2",
  "com.google.guava" % "guava" % "14.0.1"
)

resolvers += Resolver.bintrayRepo("hseeberger", "maven")
resolvers += Resolver.sonatypeRepo("snapshots")

enablePlugins(JavaServerAppPackaging)