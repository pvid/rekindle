package dev.vidlicka.spark.rekindle

import cats.implicits.*
import io.circe.*
import io.circe.generic.semiauto.*
import io.circe.parser.*
import io.circe.syntax.*

import dev.vidlicka.spark.rekindle.Observation.*

sealed trait GeneralObservation {
  val name: String
}

sealed trait Observation        extends GeneralObservation
sealed trait DerivedObservation extends GeneralObservation

object Observation {
  private val MessageKind        = "message"
  private val MetricKind         = "metric"
  private val IndexedMetricsKind = "indexedMetric"

  case class Message(
      name: String,
      contents: String,
  ) extends Observation

  case class Metric(
      name: String,
      value: Long,
  ) extends Observation

  // can be indexed by timestamp, by stage, etc
  // even stage starts/ends could be encoded using this - index would be timestamp, value the stage number
  case class IndexedMetric(
      name: String,
      index: Int,
      value: Long,
  ) extends Observation

  given Codec[Observation] = {
    val messageCodec: Codec[Message]             = deriveCodec
    val metricCodec: Codec[Metric]               = deriveCodec
    val indexedMetricCodec: Codec[IndexedMetric] = deriveCodec

    val decoder: Decoder[Observation] = Decoder.instance { cursor =>
      cursor
        .downField("kind")
        .as[String]
        .flatMap {
          case MessageKind        => messageCodec(cursor)
          case MetricKind         => metricCodec(cursor)
          case IndexedMetricsKind => indexedMetricCodec(cursor)
          case unknown =>
            DecodingFailure(s"Unknown observation kind '$unknown'", cursor.history).asLeft
        }
    }

    val encoder: Encoder[Observation] = Encoder.instance {
      case m: Message => messageCodec(m).deepMerge(Json.obj("kind" -> Json.fromString(MessageKind)))

      case m: Metric => metricCodec(m).deepMerge(Json.obj("kind" -> Json.fromString(MetricKind)))

      case m: IndexedMetric =>
        indexedMetricCodec(m).deepMerge(Json.obj("kind" -> Json.fromString(IndexedMetricsKind)))
    }

    Codec.from(decoder, encoder)
  }
}

object GeneralObservation {
  private val FractionalMetricsKind = "fractionalMetric"

  // Only derived metrics should be fractional, hence they need not be persisted
  // including this for now because of derived metrics coming from CLI app
  case class FractionalMetric(
      name: String,
      value: Double,
  ) extends DerivedObservation

  given Codec[GeneralObservation] = {
    val fractionalMetricCodec: Codec[FractionalMetric] = deriveCodec

    val decoder: Decoder[GeneralObservation] = Decoder.instance { cursor =>
      cursor
        .downField("kind")
        .as[String]
        .flatMap {
          case FractionalMetricsKind => fractionalMetricCodec(cursor)
          case _                     => Decoder[Observation].apply(cursor)
        }
    }

    val encoder: Encoder[GeneralObservation] = Encoder.instance {
      case m: FractionalMetric =>
        fractionalMetricCodec(m)
          .deepMerge(Json.obj("kind" -> Json.fromString(FractionalMetricsKind)))

      case o: Observation => o.asJson
    }

    Codec.from(decoder, encoder)
  }
}
