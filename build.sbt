val Versions = new {
  val akka = "2.5.9"
}

name := "kafka-foo"
scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % Versions.akka,
  "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % "test"
)
