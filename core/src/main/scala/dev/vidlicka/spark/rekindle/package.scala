package dev.vidlicka.spark.rekindle

import fs2.{ Pipe, Stream }
import org.apache.spark.scheduler.{ SparkListenerApplicationStart, SparkListenerEvent }

type LogLine = String

/** Replayers are the core of the whole framework. The job of the whole framework is to define
  * primitives (producing intermediate values, evaluating rules) that can be composed into a single
  * Replayer
  */
type Replayer[F[_]] = Pipe[F, SparkListenerEvent, Observation]
