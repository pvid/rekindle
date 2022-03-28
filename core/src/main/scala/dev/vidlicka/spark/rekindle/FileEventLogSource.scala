package dev.vidlicka.spark.rekindle

import cats.effect.*
import fs2.compression.*
import fs2.io.file.{ Files, Path }
import fs2.{ Pipe, Stream }

/** Source that reads event logs from the local file system */
class FileEventLogSource[F[_]: Async](paths: Seq[Path], gzipped: Boolean)
    extends EventLogSource[F] {
  import FileEventLogSource.*

  def eventLogs: Stream[F, (EventLogMetadata, Stream[F, LogLine])] = {
    Stream
      .emits(paths)
      .map { path =>
        (EventLogMetadata(path.toString), lineStream(path, gzipped))
      }
  }

}

object FileEventLogSource {
  def lineStream[F[_]: Async](path: Path, gzipped: Boolean): Stream[F, LogLine] = {
    Files[F]
      .readAll(path)
      .through(if gzipped then decompress else identity)
      .through(fs2.text.utf8.decode)
      .through(fs2.text.lines)
      .filter(_.nonEmpty)
  }

  private def decompress[F[_]: Sync]: Pipe[F, Byte, Byte] = {
    _
      .through(Compression[F].gunzip(32 * 1024))
      .flatMap(_.content)
  }
}
