package dev.vidlicka.spark.rekindle

import cats.effect.*
import fs2.Stream
import fs2.io.file.{ Files, Path }

/** Source that reads event logs from the local file system */
class FileEventLogSource[F[_]: Async](paths: Seq[Path]) extends EventLogSource[F] {
  import FileEventLogSource.*

  def eventLogs: Stream[F, (EventLogMetadata, Stream[F, LogLine])] = {
    Stream
      .emits(paths)
      .map { path =>
        (EventLogMetadata(path.toString), lineStream(path))
      }
  }

}

object FileEventLogSource {
  def lineStream[F[_]: Async](path: Path): Stream[F, LogLine] = {
    Files[F]
      .readAll(path)
      .through(fs2.text.utf8.decode)
      .through(fs2.text.lines)
      .filter(_.nonEmpty)
  }
}
