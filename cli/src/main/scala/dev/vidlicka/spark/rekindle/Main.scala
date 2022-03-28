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
    val eventLogPaths: Opts[Option[NonEmptyList[Path]]] = Opts.arguments("PATH").orNone
    val gzipped = Opts.flag("gzipped", "read input as gzip stream").orFalse

    (eventLogPaths, gzipped).tupled
      .map { case (inputPathOpt, gzipped) =>
        val source: EventLogSource[IO] = {
          inputPathOpt
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
          .through(RekindleEngine.asPipe[IO](replayer))
          .through(outputHandler)
          .compile
          .drain
          .as(ExitCode.Success)
      }
  }
}
