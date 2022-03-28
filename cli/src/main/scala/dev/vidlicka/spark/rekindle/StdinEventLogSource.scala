package dev.vidlicka.spark.rekindle

import cats.effect.*
import fs2.{ Pipe, Stream }
import fs2.compression.Compression

class StdinEventLogSource[F[_]: Sync](gzipped: Boolean) extends EventLogSource[F] {
  import StdinEventLogSource.*

  def eventLogs: Stream[F, (EventLogMetadata, Stream[F, LogLine])] = {
    Stream(
      (
        EventLogMetadata(Identifier),
        fs2
          .io
          .stdin(1024)
          .through(if gzipped then decompress else identity)
          .through(fs2.text.utf8.decode)
          .through(fs2.text.lines),
      ),
    )
  }
}

object StdinEventLogSource {
  val Identifier = "stdin"

  private def decompress[F[_]: Sync]: Pipe[F, Byte, Byte] = {
    _
      .through(Compression[F].gunzip(32 * 1024))
      .flatMap(_.content)
  }
}
