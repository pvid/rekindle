package dev.vidlicka.spark.rekindle

import cats.implicits.*
import cats.effect.*
import weaver.*

object EngineSmokeTest extends SimpleIOSuite {
  val eventLog = getClass().getResource("/event-logs/simple.log").getPath()

  test("smoke test") {
    val source = FileEventLogSource[IO](Seq(eventLog))

    source
      .eventLogs
      .through(
        RekindleEngine(
          Replayers.combine(
            LogSizeReplayer(),
            SimpleSummaryReplayer(),
          ),
          4,
        ),
      )
      .compile
      .toList
      .map { output =>
        output
          .collectFirst { case (_, Output.Metric("EventLogSize", count, _)) => count }
          .fold {
            failure("Did not found 'EventLogSize' event in output.")
          } { logSize =>
            expect(logSize > 0)
          } && expect(output.size > 1)
      }
  }
}
