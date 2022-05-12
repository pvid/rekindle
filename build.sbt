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
    cli,
    store,
    testEventLogGenerators,
  )

lazy val core = {
  project
    .settings(
      name := "core",
      settings,
      libraryDependencies ++= Seq(
        circe,
        circeGeneric,
        circeParser,
        fs2,
        fs2io,
        sparkCore,
        sparkSql,
        weaver % Test,
      ),
    )
}

lazy val cli = {
  project
    .dependsOn(core)
    .settings(
      name := "cli",
      settings,
      libraryDependencies ++= Seq(
        decline,
      ),
    )
    .enablePlugins(JavaAppPackaging)
}

lazy val store = {
  project
    .dependsOn(core)
    .settings(
      name := "store",
      settings,
      libraryDependencies ++= Seq(
        doobie,
        doobieHikari,
        h2,
        mapref,
        weaver % Test,
      ),
    )
}

lazy val testEventLogGenerators = {
  Project(id = "testEventLogGenerators", base = file("test-event-log-generators"))
    .settings(
      settings,
      libraryDependencies ++= Seq(
        sparkCore,
        sparkSql,
      ),
    )
}

lazy val settings = Seq(
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  scalacOptions ~= (_.filterNot(_ == "-Xfatal-warnings")),
)
