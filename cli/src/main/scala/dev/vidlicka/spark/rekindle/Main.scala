package dev.vidlicka.spark.rekindle

import java.nio.charset.StandardCharsets
import java.nio.file.Path

import cats.data.NonEmptyList
import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*
import com.monovore.decline.*
import com.monovore.decline.effect.*

object Main extends CommandIOApp(
      name = "rekindle",
      header = "Showcase CLI app for the Rekindle toolkit",
    ) {
  def main: Opts[IO[ExitCode]] = {
    val eventLogPaths = Opts.arguments[Path]("PATH").orNone

    val parallelism = {
      Opts.option[Int](
        "parallelism",
        help = "number of event logs processed in parallel",
      ).withDefault(4)
    }

    val gzipped = Opts.flag("gzipped", "read input as gzip stream").orFalse

    (
      eventLogPaths,
      parallelism,
      gzipped,
    ).tupled
      .map { case (eventLogPaths, parallelism, gzipped) =>
        val source: EventLogSource[IO] = {
          eventLogPaths
            .fold {
              StdinEventLogSource[IO](gzipped)
            } { paths =>
              val fs2Paths = paths.map(fs2.io.file.Path.fromNioPath)
              FileEventLogSource[IO](fs2Paths.toList, gzipped)
            }
        }

        val replayer: Replayer[IO] = Replayers.combine(
          LogSizeReplayer(),
          SimpleSummaryEnhancer(SimpleSummaryReplayer()),
        )

        val outputHandler = StdoutJsonOutputHandler[IO]

        source
          .eventLogs
          .parEvalMapUnordered(parallelism) {
            case (metadata, eventLog) =>
              RekindleEngine.process[IO](
                replayer,
                metadata,
                eventLog,
              )
                .through(outputHandler)
                .compile
                .drain
          }
          .compile
          .drain
          .as(ExitCode.Success)
      }
  }
}
