package dev.vidlicka.spark.rekindle

import fs2.Stream

trait EventLogSource[F[_]] {
  def eventLogs: Stream[F, (EventLogMetadata, Stream[F, LogLine])]
}
