package dev.vidlicka.spark.rekindle

sealed trait Output {
  def name: String
  def tags: List[String]
}

/** Most of these are just experimental and no replayer that outputs them is implemented. They try
  * to cover info/alert messages, metrics tracked across multiple app runs and single app metrics
  */
object Output {
  val EmptyTags: List[String] = List.empty

  final case class Message(name: String, contents: String, tags: List[String] = EmptyTags)
      extends Output

  // summary metric, one value per application
  final case class Metric(name: String, value: Long, tags: List[String] = EmptyTags) extends Output

  // can be indexed by timestamp, by stage, etc
  // even stage starts/ends could be encoded using this - index would be timestamp, value the stage number
  final case class IndexedMetric(
      name: String,
      index: Long,
      value: Long,
      tags: List[String] = EmptyTags,
  ) extends Output
}
