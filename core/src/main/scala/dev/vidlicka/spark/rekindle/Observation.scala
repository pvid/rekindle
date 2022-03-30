package dev.vidlicka.spark.rekindle

import dev.vidlicka.spark.rekindle.Observation.*
import io.circe.Codec

// TODO(pvid) the JSON encoding uses wrapper class. Change to discriminator one https://github.com/circe/circe/pull/1800
// is merged and published
sealed trait GeneralObservation derives Codec.AsObject {
  val name: String
}

sealed trait Observation        extends GeneralObservation
sealed trait DerivedObservation extends GeneralObservation

object Observation {
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
      index: Long,
      value: Long,
  ) extends Observation
}

object DerivedObservation {
  // Only derived metrics should be fractional, hence they need not be persisted
  // including this for now because of derived metrics coming from CLI app
  case class FractionalMetric(
      name: String,
      value: Double,
  ) extends DerivedObservation
}
