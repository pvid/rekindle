package dev.vidlicka.spark.rekindle

import cats.*
import cats.effect.*
import cats.implicits.*
import fs2.{ Pipe, Pull, Stream }
import org.apache.spark.SparkListenerEventJsonProtocol
import org.apache.spark.scheduler.{ SparkListenerApplicationStart, SparkListenerEvent }

trait RekindleEngine[F[_]: Concurrent: MonadThrow](replayer: Replayer[F], maxConcurrent: Int)
    extends Pipe[
      F,
      (EventLogMetadata, Stream[F, LogLine]),
      (ApplicationInfo, Output),
    ] {

  import RekindleEngine.*

  def apply(
      input: Stream[F, (EventLogMetadata, Stream[F, LogLine])],
  ): Stream[F, (ApplicationInfo, Output)] = {
    input
      .map { case (metadata, events) =>
        rekindleSingle(metadata, events, replayer)
      }
      .parJoin(maxConcurrent)
  }

  private def rekindleSingle(
      metadata: EventLogMetadata,
      eventLog: Stream[F, LogLine],
      replayer: Replayer[F],
  ): Stream[F, (ApplicationInfo, Output)] = {
    eventLog
      .through(parse)
      .through(parseAppInfo(metadata))
      .flatMap { case (appInfo, eventStream) =>
        eventStream
          .through(replayer)
          .handleErrorWith { error =>
            // TODO(pvid) pull out constant
            Stream.emit(Output.Message("ReplayFailed", s"Replay failed with error: $error"))
          }
          .map { output =>
            (appInfo, output)
          }
      }

  }
}

object RekindleEngine {
  val BufferForApplicationInfo = 10

  def apply[F[_]: Concurrent: MonadThrow](
      replayer: Replayer[F],
      maxConcurrent: Int,
  ): RekindleEngine[F] = {
    new RekindleEngine[F](replayer, maxConcurrent) {}
  }

  def parse[F[_]]: Pipe[F, LogLine, SparkListenerEvent] = {
    _.map(SparkListenerEventJsonProtocol.parse)
  }

  def parseAppInfo[F[_]: MonadThrow](
      metadata: EventLogMetadata,
  ): Pipe[F, SparkListenerEvent, (ApplicationInfo, Stream[F, SparkListenerEvent])] = { input =>
    bufferFirstN(input, BufferForApplicationInfo, allowFewer = true).evalMap {
      case (prefix, eventStream) =>
        prefix
          .collectFirst { case start: SparkListenerApplicationStart => start }
          .fold {
            MonadThrow[F].raiseError(
              RuntimeException(
                s"Could not find event 'SparkListenerApplicationStart' in the first ${BufferForApplicationInfo} events.",
              ),
            )
          } { startEvent =>
            val appInfo = ApplicationInfo(
              applicationId = startEvent.appId.getOrElse(""),
              attemptId = startEvent.appAttemptId.getOrElse(""),
              name = startEvent.appName,
              start = startEvent.time,
              eventLogMetada = metadata,
            )

            (appInfo, eventStream).pure[F]
          }
    }
  }

  private def bufferFirstN[F[_], A](
      input: Stream[F, A],
      n: Int,
      allowFewer: Boolean,
  ): Stream[F, (List[A], Stream[F, A])] = {
    input
      .pull
      .unconsN(n, allowFewer)
      .flatMap {
        case Some(head, tail) =>
          val prefix = head.toList
          Pull.output1((prefix, Stream.emits(prefix) ++ tail))
        case None => Pull.done
      }
      .stream
  }
}
