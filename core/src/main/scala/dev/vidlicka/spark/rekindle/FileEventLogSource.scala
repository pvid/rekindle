package dev.vidlicka.spark.rekindle

import cats.effect.*
import fs2.Stream
import fs2.io.file.{ Files, Path }

/** Source that reads event logs from the local file system */
class FileEventLogSource[F[_]: Async](paths: Seq[String]) extends EventLogSource[F] {

  override def eventLogs: Stream[F, (EventLogMetadata, Stream[F, LogLine])] = {
    Stream
      .emits(paths)
      .map { path =>
        val metadata = EventLogMetadata(path)
        (metadata, singleStream(path))
      }
  }

  def singleStream(path: String): Stream[F, LogLine] = {
    Files[F]
      .readAll(Path(path))
      .through(fs2.text.utf8.decode)
      .through(fs2.text.lines)
      .filter(_.nonEmpty)
  }
}
