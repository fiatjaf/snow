import com.google.common.escape.CharEscaperBuilder

ThisBuild / scalaVersion        := "3.3.4"
ThisBuild / organization        := "com.fiatjaf"
ThisBuild / homepage            := Some(url("https://github.com/fiatjaf/snow"))
ThisBuild / licenses            += License.Apache2
ThisBuild / developers          := List(tlGitHubDev("fiatjaf", "fiatjaf"))

ThisBuild / version := "0.0.2-SNAPSHOT"
ThisBuild / sonatypeCredentialHost := xerial.sbt.Sonatype.sonatypeLegacy

Global / onChangedBuildSource := ReloadOnSourceChanges

sonatypeProfileName    := "com.fiatjaf"
scmInfo                := Some(ScmInfo(url("https://github.com/fiatjaf/snow"), "git@github.com:fiatjaf/snow.git"))
licenses               += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
publishMavenStyle      := true
publishTo              := sonatypePublishToBundle.value
sonatypeCredentialHost := "s01.oss.sonatype.org"

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
  ).enablePlugins(ScalaJSPlugin, EsbuildPlugin)

// we need these things only to run tests on nodejs in github actions
ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v4"),
    name = Some("Setup Node.js"),
    // must be a nodejs version with native WebSocket api
    params = Map("node-version" -> "22"),
  )
ThisBuild / githubWorkflowBuildPreamble ++= Seq(
    WorkflowStep.Run(
    name = Some("Install Node Modules"),
    commands = List(
      "sbt esInstall",
      "cp -a target/esbuild/. ./"
    ),
  )
)

// maven magic, see https://github.com/makingthematrix/scala-suffix/tree/56270a#but-wait-thats-not-all
Compile / packageBin / packageOptions += Package.ManifestAttributes("Automatic-Module-Name" -> "snow")

//// below are some notes/commands which might help to just make github actions
//// entirely nix-aware:

// ThisBuild / githubWorkflowSbtCommand := "nix develop -c sbt"
// WorkflowStep.Use(
//  UseRef.Public("cachix", "install-nix-action", "v27"),
//  name = Some("Install Nix"),
// ),