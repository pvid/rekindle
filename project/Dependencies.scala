import sbt.*

object Dependencies {
  val circeVersion    = "0.14.1"
  val circeFs2Version = "0.14.0"
  val doobieVersion   = "1.0.0-RC2"
  val fs2Version      = "3.2.4"
  val sparkVersion    = "3.2.0"

  val circe        = "io.circe"            %% "circe-core"      % circeVersion
  val circeGeneric = "io.circe"            %% "circe-generic"   % circeVersion
  val circeParser  = "io.circe"            %% "circe-parser"    % circeVersion
  val decline      = "com.monovore"        %% "decline-effect"  % "2.2.0"
  val doobie       = "org.tpolecat"        %% "doobie-h2"       % doobieVersion
  val doobieHikari = "org.tpolecat"        %% "doobie-hikari"   % doobieVersion
  val fs2          = "co.fs2"              %% "fs2-core"        % fs2Version
  val fs2io        = "co.fs2"              %% "fs2-io"          % fs2Version
  val h2           = "com.h2database"       % "h2"              % "2.1.210"
  val mapref       = "io.chrisdavenport"   %% "mapref"          % "0.2.1"
  val sparkCore    = "org.apache.spark"     % "spark-core_2.13" % sparkVersion
  val sparkSql     = "org.apache.spark"     % "spark-sql_2.13"  % sparkVersion
  val weaver       = "com.disneystreaming" %% "weaver-cats"     % "0.7.4"
}
