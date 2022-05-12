package dev.vidlicka.spark.rekindle

import dev.vidlicka.spark.rekindle.Observation.Metric

trait AppQuery

object AppQuery {
  case object All extends AppQuery
}

trait ObservationQuery {
  case object All extends ObservationQuery
}

// TODO bad name
trait QueryHandler {
  def listApps(query: AppQuery): List[ApplicationInfo]

  def metrics(
      appQuery: AppQuery,
      observationQuery: ObservationQuery,
  ): List[(ApplicationInfo, Metric)]
}
