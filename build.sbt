inThisBuild(
  List(
    organization := "com.ocadotechnology",
    homepage := Some(url("https://github.com/ocadotechnology/sttp-oauth2")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "majk-p",
        "Michał Pawlik",
        "michal.pawlik@ocado.com",
        url("https://michalp.net")
      ),
      Developer(
        "matwojcik",
        "Mateusz Wójcik",
        "mateusz.wojcik@ocado.com",
        url("https://github.com/matwojcik")
      )
    ),
    versionScheme := Some("early-semver")
  )
)

val Versions = new {
  val Log4Cats = "2.2.0"
  val KamonCatsEffect = "16.0.0"
}

lazy val IntegrationTest = config("it") extend Test

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    // noPublishPlease,
    name := "pass4s",
    libraryDependencies ++= Seq(
      "com.disneystreaming" %% "weaver-cats" % "0.7.11",
      "com.disneystreaming" %% "weaver-framework" % "0.7.11",
      "com.disneystreaming" %% "weaver-scalacheck" % "0.7.11",
      "org.scalatest" %% "scalatest" % "3.2.11", // just for `shouldNot compile`
      "com.dimafeng" %% "testcontainers-scala-localstack-v2" % "0.40.4",
      "com.amazonaws" % "aws-java-sdk-core" % "1.12.189" exclude ("*", "*"), // fixme after https://github.com/testcontainers/testcontainers-java/issues/4279
      "com.dimafeng" %% "testcontainers-scala-mockserver" % "0.40.4",
      "org.mock-server" % "mockserver-client-java" % "5.13.0",
      "org.apache.activemq" % "activemq-broker" % "5.17.0",
      "org.typelevel" %% "log4cats-core" % Versions.Log4Cats,
      "org.typelevel" %% "log4cats-slf4j" % Versions.Log4Cats,
      "ch.qos.logback" % "logback-classic" % "1.2.11"
    ).map(_ % IntegrationTest),
    Defaults.itSettings,
    inConfig(IntegrationTest) {
      Defaults.testSettings
    },
    IntegrationTest / classDirectory := (Test / classDirectory).value,
    IntegrationTest / parallelExecution := true
  )
  .aggregate(core, kernel, high, activemq, kinesis, sns, sqs, circe, phobos, plaintext, extra, logging, demo, s3Proxy)
  .dependsOn(high, activemq, kinesis, sns, sqs, circe, logging, extra, s3Proxy)

def module(name: String, directory: String = ".") = Project(s"pass4s-$name", file(directory) / name).settings(commonSettings)

lazy val core = module("core")
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "co.fs2" %% "fs2-core" % "3.2.7",
      "org.typelevel" %% "cats-effect" % "3.3.11"
    )
  )

lazy val kernel = module("kernel").settings(
  libraryDependencies ++= Seq(
    "co.fs2" %% "fs2-core" % "3.2.7",
    "org.typelevel" %% "cats-effect" % "3.3.9",
    "org.typelevel" %% "cats-tagless-core" % "0.14.0",
    "org.typelevel" %% "cats-laws" % "2.7.0" % Test,
    "org.typelevel" %% "cats-effect-laws" % "3.3.11" % Test,
    "org.typelevel" %% "cats-effect-testkit" % "3.3.11" % Test,
    "com.disneystreaming" %% "weaver-discipline" % "0.7.11" % Test
  )
)

lazy val high = module("high")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect-laws" % "3.3.11" % Test
    )
  )
  .dependsOn(core, kernel)

// connectors

val awsSnykOverrides = Seq(
  "commons-codec" % "commons-codec" % "1.15"
)

lazy val activemq = module("activemq", directory = "connectors")
  .settings(
    name := "pass4s-connector-activemq",
    libraryDependencies ++= Seq(
      "com.lightbend.akka" %% "akka-stream-alpakka-jms" % "3.0.4",
      "org.apache.activemq" % "activemq-pool" % "5.17.0",
      "org.typelevel" %% "log4cats-core" % Versions.Log4Cats
    )
  )
  .dependsOn(core)

lazy val kinesis = module("kinesis", directory = "connectors")
  .settings(
    name := "pass4s-connector-kinesis",
    libraryDependencies ++= Seq(
      "io.laserdisc" %% "pure-kinesis-tagless" % "5.0.2",
      "software.amazon.awssdk" % "sts" % "2.17.155"
    ) ++ awsSnykOverrides
  )
  .dependsOn(core)

lazy val sns = module("sns", directory = "connectors")
  .settings(
    name := "pass4s-connector-sns",
    libraryDependencies ++= Seq(
      "io.laserdisc" %% "pure-sns-tagless" % "5.0.2"
    ) ++ awsSnykOverrides
  )
  .dependsOn(core)

lazy val sqs = module("sqs", directory = "connectors")
  .settings(
    name := "pass4s-connector-sqs",
    libraryDependencies ++= Seq(
      "io.laserdisc" %% "pure-sqs-tagless" % "5.0.2",
      "org.typelevel" %% "log4cats-core" % Versions.Log4Cats
    ) ++ awsSnykOverrides
  )
  .dependsOn(core)

// addons

lazy val circe = module("circe", directory = "addons")
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-parser" % "0.14.1",
      "org.typelevel" %% "jawn-parser" % "1.3.2"
    )
  )
  .dependsOn(core, kernel)

lazy val phobos = module("phobos", directory = "addons")
  .settings(
    libraryDependencies ++= Seq(
      "ru.tinkoff" %% "phobos-core" % "0.14.0"
    )
  )
  .dependsOn(core, kernel)

lazy val plaintext = module("plaintext", directory = "addons")
  .dependsOn(core, kernel)

lazy val extra = module("extra", directory = "addons")
  .dependsOn(high, circe) // TODO This should not need circe, should only rely on `high`

lazy val s3Proxy = module("s3proxy", directory = "addons")
  .settings(
    libraryDependencies ++= Seq(
      "io.laserdisc" %% "pure-s3-tagless" % "5.0.2"
    ) ++ awsSnykOverrides
  )
  .dependsOn(high, circe)

lazy val logging = module("logging", directory = "addons")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "log4cats-core" % Versions.Log4Cats
    )
  )
  .dependsOn(high)

// misc

lazy val demo = module("demo")
  .settings(
    publishArtifact := false,
    // mimaPreviousArtifacts := Set(), // TODO
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic" % "0.14.1",
      "org.typelevel" %% "log4cats-core" % Versions.Log4Cats,
      "org.typelevel" %% "log4cats-slf4j" % Versions.Log4Cats,
      "ch.qos.logback" % "logback-classic" % "1.2.11"
    )
  )
  .dependsOn(activemq, sns, sqs, extra, logging)

lazy val commonSettings = Seq(
  organization := "com.ocadotechnology",
  scalaVersion := "2.13.8",
  compilerOptions,
  Test / fork := true,
  libraryDependencies ++= compilerPlugins,
  // mimaPreviousArtifacts := Seq(), // TODO
  libraryDependencies ++= Seq(
    "com.disneystreaming" %% "weaver-cats" % "0.7.11",
    "com.disneystreaming" %% "weaver-framework" % "0.7.11",
    "com.disneystreaming" %% "weaver-scalacheck" % "0.7.11"
  ).map(_ % Test),
  testFrameworks += new TestFramework("weaver.framework.CatsEffect")
)

val compilerOptions =
  scalacOptions -= "-Xfatal-warnings"

val compilerPlugins = Seq(
  compilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
  compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)
