package dev.vidlicka.rekindle.generators

import java.nio.file.{ Files, Path, Paths }
import java.util.UUID

import org.apache.spark.sql.SparkSession

object Helpers {
  def generateEventLog(
      logDestination: Path,
      job: SparkSession => Unit,
      jobTags: List[String],
  ): Unit = {
    val tmpHistoryDirectory = Files.createTempDirectory(s"rekindle-${UUID.randomUUID.toString}")

    val session = builLocalSparkSession(tmpHistoryDirectory, jobTags)
    job(session)

    val tmpLogPath = tmpHistoryDirectory.resolve(session.sparkContext.applicationId)
    session.close()

    Files.move(tmpLogPath, logDestination)
  }

  private def builLocalSparkSession(
      historyDirectoryPath: Path,
      jobTags: List[String],
  ): SparkSession = {
    SparkSession
      .builder()
      .master("local[4]")
      .config("rekindle.tags", jobTags.mkString(","))
      .config("spark.eventLog.enabled", true)
      .config("spark.eventLog.dir", s"file://${historyDirectoryPath.toString}")
      .getOrCreate()
  }
}
