name := "watcher"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.4" % "runtime",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2"
)

checksums in update := Nil
