package dev.vidlicka.spark.rekindle.rehydrate

import cats.effect.*
import cats.implicits.*
import fs2.Stream
import fs2.io.file.{ Files, Path }
import weaver.*

import dev.vidlicka.spark.rekindle.*
import dev.vidlicka.spark.rekindle.replayers.*

object RehydrationTest extends SimpleIOSuite {
  val eventLogPath = getClass().getResource("/event-logs/simple.log").getPath

  test("Rehydrating produces the same output as re-running rekindle") {
    val eventLog = FileEventLogSource.lineStream[IO](Path(eventLogPath), false)

    val outputStream = {
      RekindleEngine.process[IO](
        Replayers.combine(
          canonicalReplayers *,
        ),
        EventLogMetadata("smoke-test"),
        eventLog,
      )
    }

    Files[IO]
      .tempDirectory
      .use { dir =>
        val dir = Path("./testdir")
        for {
          _          <- ObservationsRehydrator.dehydrate(1, dir, outputStream)
          dehydrated <- buffer(ObservationsRehydrator.rehydrate(dir))
          buffered   <- buffer(outputStream)
        } yield {
          expect.same(dehydrated, buffered) && expect(dehydrated.size > 0)
        }
      }
  }

  private def buffer(
      in: Stream[IO, (ApplicationInfo, Stream[IO, Observation])],
  ): IO[Set[(ApplicationInfo, Observation)]] = {
    in
      .flatMap { case (appInfo, observations) =>
        observations
          .map((appInfo, _))
      }
      .compile
      .toList
      .map(_.toSet)
  }
}
