ThisBuild / scalaVersion := "3.3.4"
ThisBuild / developers := List(
  tlGitHubDev("fiatjaf", "fiatjaf"),
  tlGitHubDev("vzxplnhqr", "vzxplnhqr")
)
ThisBuild / organizationName := "fiatjaf" 
ThisBuild / organizationHomepage := Some(url("https://fiatjaf.com"))
ThisBuild / homepage := Some(url("https://github.com/fiatjaf/snow"))
ThisBuild / startYear := Some(2023)

/**
  * the following settings need to be changed (commented/uncommmented) depending
  * on which developer is publishing the releases via sonatype via github ci
  */
//ThisBuild / organization := "com.fiatjaf"                     // fiatjaf
//ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"  // fiatjaf
ThisBuild / organization           := "io.github.vzxplnhqr"   // VzxPLnHqr
ThisBuild / sonatypeCredentialHost := "central.sonatype.com"  // VzxPLnHqr

// see: https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.0"

// for publishing snapshots
// ThisBuild / tlCiReleaseBranches := Seq("master")

// we disable these things for now so that github ci does not barf
ThisBuild / tlCiHeaderCheck := false
ThisBuild / tlCiScalafixCheck := false
ThisBuild / tlCiScalafmtCheck := false

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / scalacOptions ++= Seq("-source:future")

// using sbt-typelevel plugin compiler fatal warnings are on by default
ThisBuild / tlFatalWarnings := false

ThisBuild / publishMavenStyle      := true
ThisBuild / publishTo              := sonatypePublishToBundle.value

lazy val snow = project
  .in(file("."))
  .settings(
    name := "snow",
    description := "Scala Nostr W̶a̶r̶s̶h̶i̶p̶s̶ Utilities",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.14.5",
      "io.circe" %%% "circe-generic" % "0.14.5",
      "io.circe" %%% "circe-parser" % "0.14.5",
      "com.fiatjaf" %%% "scoin" % "0.7.0",
      "org.http4s" %%% "http4s-dom" % "0.2.11",
      "org.http4s" %%% "http4s-client" % "0.23.25",
      "com.lihaoyi" %%% "utest" % "0.8.0" % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )
  .enablePlugins(ScalaJSPlugin, EsbuildPlugin)

// we need these things only to run tests on nodejs in github actions
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("21"))
ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v4"),
    name = Some("Setup Node.js"),
    // must be a nodejs version with native WebSocket api
    params = Map("node-version" -> "22")
  )
ThisBuild / githubWorkflowBuildPreamble ++= Seq(
  WorkflowStep.Run(
    name = Some("Install Node Modules"),
    commands = List(
      "sbt esInstall",
      "cp -a target/esbuild/. ./"
    )
  )
)

// maven magic, see https://github.com/makingthematrix/scala-suffix/tree/56270a#but-wait-thats-not-all
Compile / packageBin / packageOptions += Package.ManifestAttributes(
  "Automatic-Module-Name" -> "snow"
)

//// below are some notes/commands which might help to just make github actions
//// entirely nix-aware:

// ThisBuild / githubWorkflowSbtCommand := "nix develop -c sbt"
// WorkflowStep.Use(
//  UseRef.Public("cachix", "install-nix-action", "v27"),
//  name = Some("Install Nix"),
// ),
