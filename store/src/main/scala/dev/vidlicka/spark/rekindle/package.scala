package dev.vidlicka.spark.rekindle

import dev.vidlicka.spark.rekindle.Observation.Metric

enum ObservationKind {
  case Metric        extends ObservationKind
  case IndexedMetric extends ObservationKind
  case Message       extends ObservationKind

  def asString: String = {
    this match {
      case Metric        => "metric"
      case IndexedMetric => "indexed_metric"
      case Message       => "message"
    }
  }
}

object ObservationKind {
  def forObservation(observation: Observation): ObservationKind = {
    observation match {
      case m: Observation.Metric        => ObservationKind.Metric
      case m: Observation.IndexedMetric => ObservationKind.IndexedMetric
      case m: Observation.Message       => ObservationKind.Message
    }
  }
}

case class ObservationIdentifier(name: String, kind: ObservationKind)

object ObservationIdentifier {
  def forObservation(observation: Observation): ObservationIdentifier = {
    ObservationIdentifier(observation.name, ObservationKind.forObservation(observation))
  }
}

case class CompactMetric(appId: Long, observationId: Int, value: Long)
case class CompactIndexedMetric(appId: Long, observationId: Int, index: Int, value: Long)
case class CompactMessage(appId: Long, observationId: Int, message: String)
