package dev.vidlicka.spark.rekindle.replayers

import fs2.Stream
import org.apache.spark.scheduler.{ SparkListenerEvent, SparkListenerStageCompleted }

import dev.vidlicka.spark.rekindle.*

// TODO(pvid) does not handle failed stages properly, see attemptId fields
class StageDurationReplayer[F[_]] extends Replayer[F] {
  def apply(events: Stream[F, SparkListenerEvent]): Stream[F, Observation] = {
    events
      .collect { case e: SparkListenerStageCompleted => e }
      .map { completed =>
        (
          for {
            start <- completed.stageInfo.submissionTime
            end   <- completed.stageInfo.completionTime
          } yield {
            end - start
          }
        )
          .fold {
            Observation.Message(
              "StageInfoIncomplete",
              s"Stage ${completed.stageInfo.stageId} does not have submission or completion time",
            )
          } { duration =>
            Observation.IndexedMetric("StageDuration", completed.stageInfo.stageId, duration)
          }
      }
  }
}
