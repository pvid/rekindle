package dev.vidlicka.spark.rekindle

import dev.vidlicka.spark.rekindle.Output.*
import io.circe.Codec

// Most of these are just experimental and no replayer that outputs them is implemented. They try
// to cover info/alert messages, metrics tracked across multiple app runs and single app metrics
// TODO(pvid) the JSON encoding uses wrapper class. Change to discriminator one https://github.com/circe/circe/pull/1800
// is merged and published
enum Output(name: String, tags: List[String]) derives Codec.AsObject {
  case Message(
      name: String,
      contents: String,
      tags: List[String] = EmptyTags,
  ) extends Output(name, tags)

  case Metric(
      name: String,
      value: Long,
      tags: List[String] = EmptyTags,
  ) extends Output(name, tags)

  // can be indexed by timestamp, by stage, etc
  // even stage starts/ends could be encoded using this - index would be timestamp, value the stage number
  case IndexedMetric(
      name: String,
      index: Long,
      value: Long,
      tags: List[String] = EmptyTags,
  ) extends Output(name, tags)
}

object Output {
  val EmptyTags: List[String] = List.empty
}
