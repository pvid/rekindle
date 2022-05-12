package dev.vidlicka.spark.rekindle.replayers

import fs2.Stream
import org.apache.spark.scheduler.{ SparkListenerEvent, SparkListenerLogStart }

import dev.vidlicka.spark.rekindle.*

// TODO this is quite boring
class SparkVersionReplayer[F[_]] extends Replayer[F] {
  def apply(events: Stream[F, SparkListenerEvent]): Stream[F, Observation] = {
    events
      .collect { case e: SparkListenerLogStart => e }
      .map { logStart =>
        Observation.Message("SparkVersion", logStart.sparkVersion)
      }
  }
}
