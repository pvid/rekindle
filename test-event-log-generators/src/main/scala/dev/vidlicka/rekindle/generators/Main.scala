package dev.vidlicka.rekindle.generators

import java.nio.file.{ Files, Path, Paths }
import java.util.UUID

import org.apache.spark.sql.SparkSession

/** Generator of Spark event logs for tests
  *
  * The code here is full of unconstraint side effects. It is basically a collection of scripts used
  * to generate event logs to be used in rekindle tests. It is not meant routinely re-run as the
  * event logs used in tests are to be commited into VCS.
  */
object Main {
  type JobIdentifier = String

  val TestJobs: Map[JobIdentifier, SparkSession => Unit] = {
    Map("simple" -> simple)
  }

  def main(args: Array[String]): Unit = {
    args match {
      case Array(pathCandidate) => {
        main(Paths.get(pathCandidate), TestJobs.keys.toList)
      }

      case Array(pathCandidate, jobs) => {
        main(Paths.get(pathCandidate), jobs.split(",").toList)
      }
    }
  }

  def main(destinationPath: Path, jobIdentifiers: List[String]): Unit = {
    jobIdentifiers.foreach { jobIdentifier =>
      Helpers.generateEventLog(
        logDestination = destinationPath.resolve("simple.log"),
        job = TestJobs(jobIdentifier),
        jobTags = List(s"test-job-identifier:${jobIdentifier}"),
      )
    }
  }

  def simple(session: SparkSession): Unit = {
    session
      .sparkContext
      .range(0, 10000)
      .repartition(200)
      .map(x => x * x)
      .keyBy(_ % 3)
      .reduceByKey(_ + _)
      .collect()
  }
}
