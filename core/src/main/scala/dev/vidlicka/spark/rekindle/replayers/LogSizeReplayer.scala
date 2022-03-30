package dev.vidlicka.spark.rekindle

import fs2.Stream
import org.apache.spark.scheduler.SparkListenerEvent

class LogSizeReplayer[F[_]] extends Replayer[F] {

  def apply(events: Stream[F, SparkListenerEvent]): Stream[F, Observation] = {
    events
      .fold(0L) { case (acc, _) =>
        acc + 1
      }
      .map { count =>
        Observation.Metric("EventLogSize", count): Observation
      }
  }
}
