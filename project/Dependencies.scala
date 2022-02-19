import sbt.*

object Dependencies {
  val sparkVersion = "3.2.0"
  val fs2Verion    = "3.2.4"

  val fs2       = "co.fs2"              %% "fs2-core"        % fs2Verion
  val fs2io     = "co.fs2"              %% "fs2-io"          % fs2Verion
  val sparkCore = "org.apache.spark"     % "spark-core_2.13" % sparkVersion
  val sparkSql  = "org.apache.spark"     % "spark-sql_2.13"  % sparkVersion
  val weaver    = "com.disneystreaming" %% "weaver-cats"     % "0.7.4"
}
