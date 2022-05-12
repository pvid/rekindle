package dev.vidlicka.spark.rekindle

import cats.effect.*
import cats.implicits.*
import fs2.{ Pipe, Stream }
import org.apache.spark.scheduler.SparkListenerEvent

import dev.vidlicka.spark.rekindle.replayers.SimpleSummaryReplayer.Metrics

/** Enhance outputs coming from SimpleSummaryReplayer by a few derived metrics
  *
  * NOTE: it buffers the whole output and is generally not very performant!
  */
class SimpleSummaryEnhancer[F[_]: Concurrent] extends Pipe[F, Observation, GeneralObservation] {
  def apply(eventLog: Stream[F, Observation]): Stream[F, GeneralObservation] = {
    eventLog
      .through(enhance)
  }

  private def enhance: Pipe[F, Observation, GeneralObservation] = { out =>
    // quick'n'dirty (and not at all efficienr)
    Stream.evalSeq {
      for {
        outputs <- out.compile.toList
        derivedObservations = derive(valuesMap(outputs))
      } yield {
        outputs ++ derivedObservations
      }
    }
  }

  private def valuesMap(outputs: List[Observation]): Map[String, Long] = {
    outputs
      .collect {
        case Observation.Metric(name, value) => name -> value
      }
      .toMap
  }

  private def derive(valuesMap: Map[String, Long]): List[DerivedObservation] = {
    val gcRatio: Option[DerivedObservation] = {
      for {
        taskTime <- valuesMap.get(Metrics.TotalTaskTime)
        gcTime   <- valuesMap.get(Metrics.TotalGCTime)
      } yield {
        GeneralObservation.FractionalMetric("GCRatio", gcTime.toDouble / taskTime)
      }
    }

    val effectiveParallelism: Option[DerivedObservation] = {
      for {
        taskTime <- valuesMap.get(Metrics.TotalTaskTime)
        duration <- valuesMap.get(Metrics.Duration)
      } yield {
        GeneralObservation.FractionalMetric("EffectiveParallelism", taskTime.toDouble / duration)
      }
    }

    List(
      gcRatio,
      effectiveParallelism,
    )
      .flatten
  }
}
