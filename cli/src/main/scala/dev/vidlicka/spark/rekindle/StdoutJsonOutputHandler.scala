package dev.vidlicka.spark.rekindle

import cats.effect.Sync
import fs2.Stream
import io.circe.syntax.*

class StdoutJsonOutputHandler[F[_]: Sync] extends OutputHandler[F] {
  def apply(
      outputStream: Stream[F, (ApplicationInfo, Stream[F, Output])],
  ): Stream[F, Unit] = {
    outputStream
      .map { case (appInfo, outputStream) =>
        // WARNING a hacked together "streaming" JSON encoder follow
        // it is brittle, it is misguided, but it works well enough
        Stream(
          """{"applicationInfo":""",
          appInfo.asJson.noSpaces,
          ""","output":[""",
        ) ++
          outputStream.map(_.asJson.noSpaces).intersperse(",") ++
          Stream(
            "]}\n",
          )
      }
      .flatten
      .through(fs2.text.utf8.encode)
      .through(fs2.io.stdout)
  }
}
