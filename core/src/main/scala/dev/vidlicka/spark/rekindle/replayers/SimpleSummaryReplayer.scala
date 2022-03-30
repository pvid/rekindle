package dev.vidlicka.spark.rekindle

import fs2.Stream
import org.apache.spark.scheduler.{
  SparkListenerApplicationEnd,
  SparkListenerApplicationStart,
  SparkListenerEvent,
  SparkListenerJobStart,
  SparkListenerStageSubmitted,
  SparkListenerTaskEnd,
  SparkListenerTaskStart,
}

object SimpleSummaryReplayer {
  final private case class State(
      var appStart: Option[Long] = None,
      var duration: Long = -1,
      var jobCount: Int = 0,
      var stageCount: Int = 0,
      var taskCount: Int = 0,
      var totalTaskTime: Long = 0,
      var totalGCTime: Long = 0,
  )

  object Metrics {
    val Duration      = "Duration"
    val JobCount      = "JobCount"
    val StageCount    = "StageCount"
    val TaskCount     = "TaskCount"
    val TotalTaskTime = "TotalTaskTime"
    val TotalGCTime   = "TotalGCTime"
  }
}

class SimpleSummaryReplayer[F[_]] extends Replayer[F] {

  import SimpleSummaryReplayer.*

  def apply(eventLog: Stream[F, SparkListenerEvent]): Stream[F, Observation] = {
    eventLog
      .fold[State](State()) { case (state, event) =>
        event match {
          case start: SparkListenerApplicationStart =>
            state.appStart = Some(start.time)

          case _: SparkListenerJobStart =>
            state.jobCount += 1

          case _: SparkListenerStageSubmitted =>
            state.stageCount += 1

          case _: SparkListenerTaskStart =>
            state.taskCount += 1

          case taskEnd: SparkListenerTaskEnd => {
            state.totalTaskTime += taskEnd.taskInfo.duration
            state.totalGCTime += taskEnd.taskMetrics.jvmGCTime
          }

          case end: SparkListenerApplicationEnd =>
            state
              .appStart
              .foreach { start =>
                state.duration = end.time - start
              }
          case _ => ()
        }
        state
      }
      .flatMap { state =>
        Stream.emits(
          Seq(
            Observation.Metric(Metrics.Duration, state.duration),
            Observation.Metric(Metrics.JobCount, state.jobCount.toLong),
            Observation.Metric(Metrics.StageCount, state.stageCount.toLong),
            Observation.Metric(Metrics.TaskCount, state.taskCount.toLong),
            Observation.Metric(Metrics.TotalTaskTime, state.totalTaskTime),
            Observation.Metric(Metrics.TotalGCTime, state.totalGCTime),
          ),
        )
      }
  }
}
