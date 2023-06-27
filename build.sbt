ThisBuild / scalaVersion        := "3.3.0"
ThisBuild / organization        := "com.fiatjaf"
ThisBuild / homepage            := Some(url("https://github.com/fiatjaf/snow"))
ThisBuild / licenses            += License.Apache2
ThisBuild / developers          := List(tlGitHubDev("fiatjaf", "fiatjaf"))

ThisBuild / version := "0.0.1"
ThisBuild / tlSonatypeUseLegacyHost := false

Global / onChangedBuildSource := ReloadOnSourceChanges

sonatypeProfileName    := "com.fiatjaf"
homepage               := Some(url("https://github.com/fiatjaf/snow"))
scmInfo                := Some(ScmInfo(url("https://github.com/fiatjaf/snow"), "git@github.com:fiatjaf/snow.git"))
licenses               += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
developers             := List(
  Developer(id="fiatjaf", name="fiatjaf", email="fiatjaf@gmail.com", url=url("https://fiatjaf.com/")),
)
publishMavenStyle      := true
publishTo              := sonatypePublishToBundle.value
sonatypeCredentialHost := "s01.oss.sonatype.org"

lazy val snow = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("."))
  .settings(
    name := "snow",
    description := "Scala Nostr W̶a̶r̶s̶h̶i̶p̶s̶ Utilities",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.14.5",
      "io.circe" %%% "circe-generic" % "0.14.5",
      "io.circe" %%% "circe-parser" % "0.14.5",
      "org.http4s" %%% "http4s-client" % "1.0.0-M36",
      "com.fiatjaf" %%% "scoin" % "0.7.0",

      "com.lihaoyi" %%% "utest" % "0.8.0" % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-dom" % "1.0.0-M36",
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

// maven magic, see https://github.com/makingthematrix/scala-suffix/tree/56270a#but-wait-thats-not-all
Compile / packageBin / packageOptions += Package.ManifestAttributes("Automatic-Module-Name" -> "snow")
