package dev.vidlicka.spark

import fs2.Pipe
import org.apache.spark.scheduler.{ SparkListenerApplicationStart, SparkListenerEvent }

package object rekindle {
  type LogLine = String

  /** Replayers are the core of the whole framework. The job of the whole framework is to define
    * primitives (producing intermediate values, evaluating rules) that can be composed into a
    * single Replayer
    */
  type Replayer[F[_]] = Pipe[F, SparkListenerEvent, Output]

  val event: SparkListenerApplicationStart = ???
}
