package dev.vidlicka.spark.rekindle

import cats.*
import cats.effect.*
import cats.implicits.*
import fs2.{ Pipe, Pull, Stream }
import org.apache.spark.SparkListenerEventJsonProtocol
import org.apache.spark.scheduler.{ SparkListenerApplicationStart, SparkListenerEvent }

object RekindleEngine {
  val LogLinesToBufferForAppInfo = 10

  def asPipe[F[_]: Concurrent: MonadThrow](
      replayer: Replayer[F],
  ): Pipe[F, (EventLogMetadata, Stream[F, LogLine]), (ApplicationInfo, Stream[F, Observation])] = {
    _
      .flatMap { case (metadata, eventLog) =>
        RekindleEngine.process[F](
          replayer,
          metadata,
          eventLog,
        ),
      }
  }

  def process[F[_]: Concurrent: MonadThrow](
      replayer: Replayer[F],
      metadata: EventLogMetadata,
      eventLog: Stream[F, LogLine],
  ): Stream[F, (ApplicationInfo, Stream[F, Observation])] = {
    eventLog
      .through(parse)
      .through(parseAppInfo(metadata))
      .map { case (appInfo, eventStream) =>
        val outputs = eventStream
          .through(replayer)
          .handleErrorWith { error =>
            // TODO(pvid) pull out constant
            Stream.emit(Observation.Message("ReplayFailed", s"Replay failed with error: $error"))
          }

        (appInfo, outputs)
      }
  }

  def parse[F[_]]: Pipe[F, LogLine, SparkListenerEvent] = {
    _.map(SparkListenerEventJsonProtocol.parse)
  }

  def parseAppInfo[F[_]: MonadThrow](
      metadata: EventLogMetadata,
  ): Pipe[F, SparkListenerEvent, (ApplicationInfo, Stream[F, SparkListenerEvent])] = { input =>
    bufferFirstN(input, LogLinesToBufferForAppInfo, allowFewer = true).evalMap {
      case (prefix, eventStream) =>
        prefix
          .collectFirst { case start: SparkListenerApplicationStart => start }
          .fold {
            MonadThrow[F].raiseError(
              RuntimeException(
                s"Could not find event 'SparkListenerApplicationStart' in the first ${LogLinesToBufferForAppInfo} events.",
              ),
            )
          } { startEvent =>
            val appInfo = ApplicationInfo(
              applicationId = startEvent.appId.getOrElse(""),
              attemptId = startEvent.appAttemptId.getOrElse(""),
              name = startEvent.appName,
              start = startEvent.time,
              eventLogMetadata = metadata,
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
    input.pull
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
