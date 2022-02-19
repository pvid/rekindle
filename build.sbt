import Dependencies.*

ThisBuild / scalaVersion := "3.1.1"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "dev.vidlicka"

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

lazy val root = (project in file("."))
  .settings(settings)
  .aggregate(
    core,
  )

lazy val core = project
  .settings(
    name := "core",
    settings,
    libraryDependencies ++= Seq(
      fs2,
      fs2io,
      sparkCore,
      sparkSql,
      weaver % Test,
    ),
  )

lazy val settings = Seq(
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  scalacOptions ~= (_.filterNot(_ == "-Xfatal-warnings")),
)
