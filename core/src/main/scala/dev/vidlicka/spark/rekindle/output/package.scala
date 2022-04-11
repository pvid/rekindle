package dev.vidlicka.spark.rekindle.output

import fs2.{ Pipe, Stream }
import io.circe.syntax.*

import dev.vidlicka.spark.rekindle.*
import io.circe.Codec

class StreamingJsonEncoder[F[_]]
    extends Pipe[F, (ApplicationInfo, Stream[F, GeneralObservation]), Byte] {
  def apply(
      outputs: Stream[F, (ApplicationInfo, Stream[F, GeneralObservation])],
  ): Stream[F, Byte] = {
    outputs
      .map(StreamingJsonEncoder.encodeSingle)
      .flatten
      .through(fs2.text.utf8.encode)
  }
}

object StreamingJsonEncoder {
  case class Format(
      applicationInfo: ApplicationInfo,
      output: List[Observation],
  ) derives Codec.AsObject

  // WARNING this is a hacked together "streaming" JSON encoder.
  // It is brittle, it is misguided, but it works well enough
  def encodeSingle[F[_]](
      appInfo: ApplicationInfo,
      observations: Stream[F, GeneralObservation],
  ): Stream[F, String] = {
    Stream(
      """{"applicationInfo":""",
      appInfo.asJson.noSpaces,
      ""","output":[""",
    ) ++
      observations.map(_.asJson.noSpaces).intersperse(",") ++
      Stream(
        "]}\n",
      )
  }
}
