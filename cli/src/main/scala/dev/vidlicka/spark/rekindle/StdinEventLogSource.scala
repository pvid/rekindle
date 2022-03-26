package dev.vidlicka.spark.rekindle

import cats.effect.*
import fs2.Stream

class StdinEventLogSource[F[_]: Sync] extends EventLogSource[F] {
  import StdinEventLogSource.*

  def eventLogs: Stream[F, (EventLogMetadata, Stream[F, LogLine])] = {
    Stream(
      (
        EventLogMetadata(Identifier),
        fs2
          .io
          .stdinUtf8(256)
          .through(fs2.text.lines),
      ),
    )
  }
}

object StdinEventLogSource {
  val Identifier = "stdin"
}
