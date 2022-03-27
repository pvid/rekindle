package dev.vidlicka.spark.rekindle

import dev.vidlicka.spark.rekindle.Output.*
import io.circe.Codec

// Most of these are just experimental and no replayer that outputs them is implemented. They try
// to cover info/alert messages, metrics tracked across multiple app runs and single app metrics
// TODO(pvid) the JSON encoding uses wrapper class. Change to discriminator one https://github.com/circe/circe/pull/1800
// is merged and published
// TODO(pvid) maybe rename to something other than Output, since for storage and server part, it is very much not an output
enum Output(name: String) derives Codec.AsObject {
  case Message(
      name: String,
      contents: String,
  ) extends Output(name)

  case Metric(
      name: String,
      value: Long,
  ) extends Output(name)

  // can be indexed by timestamp, by stage, etc
  // even stage starts/ends could be encoded using this - index would be timestamp, value the stage number
  case IndexedMetric(
      name: String,
      index: Long,
      value: Long,
  ) extends Output(name)

  // Only derived metrics should be fractional, hence they need not be persisted
  // including this for now because of derived metrics coming from CLI app
  case FractionalMetric(
      name: String,
      value: Double,
  ) extends Output(name)
}
