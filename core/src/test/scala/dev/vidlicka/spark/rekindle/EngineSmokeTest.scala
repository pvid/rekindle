package dev.vidlicka.spark.rekindle

import cats.effect.*
import cats.implicits.*
import fs2.io.file.Path
import weaver.*

object EngineSmokeTest extends SimpleIOSuite {
  val eventLogPath = getClass().getResource("/event-logs/simple.log").getPath

  test("smoke test") {
    val eventLog = FileEventLogSource.lineStream[IO](Path(eventLogPath), false)

    val outputStream = {
      RekindleEngine.process[IO](
        Replayers.combine(
          LogSizeReplayer(),
          SimpleSummaryReplayer(),
        ),
        EventLogMetadata("smoke-test"),
        eventLog,
      )
    }

    TestHelpers
      .withCollectedObservationsForSingleApp(outputStream) { case (_, observations) =>
        expect(observations.size > 1) && {
          observations.collectFirst {
            case Observation.Metric("EventLogSize", count) =>
              count
          }.fold {
            failure("Did not found 'EventLogSize' event in output.")
          } { logSize =>
            expect(logSize > 0)
          }
        }
      }
  }
}
