import sbt.*

object Dependencies {
  val circeVersion = "0.14.1"
  val fs2Version   = "3.2.4"
  val sparkVersion = "3.2.0"

  val circe     = "io.circe"            %% "circe-core"      % circeVersion
  val decline   = "com.monovore"        %% "decline-effect"  % "2.2.0"
  val fs2       = "co.fs2"              %% "fs2-core"        % fs2Version
  val fs2io     = "co.fs2"              %% "fs2-io"          % fs2Version
  val sparkCore = "org.apache.spark"     % "spark-core_2.13" % sparkVersion
  val sparkSql  = "org.apache.spark"     % "spark-sql_2.13"  % sparkVersion
  val weaver    = "com.disneystreaming" %% "weaver-cats"     % "0.7.4"
}
