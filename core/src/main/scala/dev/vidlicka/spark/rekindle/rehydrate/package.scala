package dev.vidlicka.spark.rekindle.rehydrate

import cats.*
import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*
import fs2.compression.Compression
import fs2.io.file.{ Files, Path }
import fs2.{ Pipe, Stream }
import io.circe.parser.*
import io.circe.syntax.*

import dev.vidlicka.spark.rekindle.*
import dev.vidlicka.spark.rekindle.output.StreamingJsonEncoder

// not especially efficient - needs to read into memory whole output for a single app
class GeneralStreamingDecoder[F[_]: Sync]
    extends Pipe[F, Byte, (ApplicationInfo, Stream[F, GeneralObservation])] {

  def apply(input: Stream[F, Byte]): Stream[F, (ApplicationInfo, Stream[F, GeneralObservation])] = {
    input
      .through(fs2.text.utf8.decode)
      .through(fs2.text.lines)
      .filter(_.nonEmpty)
      .map(decode[StreamingJsonEncoder.Format])
      .rethrow
      .map { case format =>
        (format.applicationInfo, Stream.emits(format.output))
      }
  }
}
class StreamingDecoder[F[_]: Sync]
    extends Pipe[F, Byte, (ApplicationInfo, Stream[F, Observation])] {
  def apply(input: Stream[F, Byte]): Stream[F, (ApplicationInfo, Stream[F, Observation])] = {
    input
      .through(GeneralStreamingDecoder())
      .map { case (appInfo, obsStream) =>
        (
          appInfo,
          obsStream
            .collect { case o: Observation => o },
        )
      }
  }
}

object ObservationsRehydrator {
  def rehydrate[F[_]: Async](
      directory: Path,
  ): Stream[F, (ApplicationInfo, Stream[F, Observation])] = {
    Files[F]
      .list(directory)
      .filter(_.fileName.toString.endsWith(".json.gz"))
      .flatMap(rehydrateSingle)
  }

  def rehydrateSingle[F[_]: Async](
      path: Path,
  ): Stream[F, (ApplicationInfo, Stream[F, Observation])] = {
    Files[F]
      .readAll(path)
      .through(decompress)
      .through(StreamingDecoder[F])
  }
  private def decompress[F[_]: Sync]: Pipe[F, Byte, Byte] = {
    _
      .through(Compression[F].gunzip())
      .flatMap(_.content)
  }

  def dehydrate[F[_]: Async: Concurrent](
      parallelism: Int,
      directory: Path,
      observations: Stream[F, (ApplicationInfo, Stream[F, Observation])],
  ): F[Unit] = {
    Files[F]
      .createDirectories(directory) *> {
      observations
        .zipWithIndex
        .parEvalMapUnordered(parallelism) { case ((appInfo, observations), index) =>
          StreamingJsonEncoder
            .encodeSingle(appInfo, observations)
            .through(fs2.text.utf8.encode)
            .through(Compression[F].gzip())
            .through(Files[F].writeAll(directory.resolve(s"${index}.json.gz")))
            .compile
            .drain
        }
        .compile
        .drain
    }
  }
}
