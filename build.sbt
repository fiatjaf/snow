ThisBuild / scalaVersion        := "3.2.1"
ThisBuild / organization        := "com.fiatjaf"
ThisBuild / homepage            := Some(url("https://github.com/fiatjaf/snow"))
ThisBuild / licenses            += License.Apache2
ThisBuild / developers          := List(tlGitHubDev("fiatjaf", "fiatjaf"))

ThisBuild / version := "0.0.1"
ThisBuild / tlSonatypeUseLegacyHost := false

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = project
  .in(file("."))
  .settings(
    name := "snow",
    description := "Scala Nostr W̶a̶r̶s̶h̶i̶p̶s̶ Utilities",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.14.3",
      "io.circe" %%% "circe-generic" % "0.14.3",
      "io.circe" %%% "circe-parser" % "0.14.3",
      "com.fiatjaf" %%% "scoin" % "0.5.0",
      "org.http4s" %%% "http4s-client" % "1.0.0-M36",
      "org.http4s" %%% "http4s-dom" % "1.0.0-M36",

      "com.lihaoyi" %%% "utest" % "0.8.0" % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

enablePlugins(ScalaJSPlugin, EsbuildPlugin)
