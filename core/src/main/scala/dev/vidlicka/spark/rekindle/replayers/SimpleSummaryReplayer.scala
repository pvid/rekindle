package dev.vidlicka.spark.rekindle

import fs2.Stream
import org.apache.spark.scheduler.{
  SparkListenerApplicationEnd,
  SparkListenerApplicationStart,
  SparkListenerEvent,
  SparkListenerJobStart,
  SparkListenerStageSubmitted,
  SparkListenerTaskStart,
}

object SimpleSummaryReplayer {
  final private case class State(
      var appStart: Option[Long] = None,
      var duration: Long = -1,
      var jobCount: Int = 0,
      var stageCount: Int = 0,
      var taskCount: Int = 0,
  )
}

class SimpleSummaryReplayer[F[_]] extends Replayer[F] {

  import SimpleSummaryReplayer.State

  override def apply(
      eventLog: Stream[F, SparkListenerEvent],
  ): Stream[F, Output] = {
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
            // TODO(pvid) pull out constants
            Output.Metric("Duration", state.duration),
            Output.Metric("JobCount", state.jobCount.toLong),
            Output.Metric("StageCount", state.stageCount.toLong),
            Output.Metric("TaskCount", state.taskCount.toLong),
          ),
        )
      }
  }
}
