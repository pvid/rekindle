package dev.vidlicka.spark.rekindle

import cats.effect.Sync
import fs2.{ Pipe, Stream }
import io.circe.syntax.*

import dev.vidlicka.spark.rekindle.output.*

class StdoutJsonOutputHandler[F[_]: Sync]
    extends Pipe[F, (ApplicationInfo, Stream[F, GeneralObservation]), Unit] {
  def apply(
      outputStream: Stream[F, (ApplicationInfo, Stream[F, GeneralObservation])],
  ): Stream[F, Unit] = {
    outputStream
      .through(StreamingJsonEncoder())
      .through(fs2.io.stdout)
  }
}
