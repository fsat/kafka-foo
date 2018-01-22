val Versions = new {
  val akka = "2.5.9"
  val reactiveKafka = "0.18"
  val scalaTest = "3.0.1"
}

name := "kafka-foo"
scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % Versions.akka,
  "com.typesafe.akka" %% "akka-stream-kafka" % Versions.reactiveKafka,
  "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % "test",
  "org.scalatest" %% "scalatest" % Versions.scalaTest % "test"
)
