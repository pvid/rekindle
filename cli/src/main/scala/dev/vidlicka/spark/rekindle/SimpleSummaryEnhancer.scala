package dev.vidlicka.spark.rekindle

import cats.effect.*
import cats.implicits.*
import fs2.{ Pipe, Stream }
import org.apache.spark.scheduler.SparkListenerEvent
import dev.vidlicka.spark.rekindle.SimpleSummaryReplayer.Metrics

/** Enhance outputs coming from SimpleSummaryReplayer by a few derived metrics */
class SimpleSummaryEnhancer[F[_]: Concurrent](summaryReplayer: SimpleSummaryReplayer[F])
    extends Replayer[F] {
  def apply(eventLog: Stream[F, SparkListenerEvent]): Stream[F, Output] = {
    eventLog
      .through(summaryReplayer)
      .through(enhance)
  }

  private def enhance: Pipe[F, Output, Output] = { out =>
    // quick'n'dirty (and not at all efficienr)
    Stream.evalSeq {
      for {
        outputs <- out.compile.toList
        derivedOutputs = derive(valuesMap(outputs))
      } yield {
        outputs ++ derivedOutputs
      }
    }
  }

  private def valuesMap(outputs: List[Output]): Map[String, Long] = {
    outputs
      .collect {
        case Output.Metric(name, value) => name -> value
      }
      .toMap
  }

  private def derive(valuesMap: Map[String, Long]): List[Output] = {
    val gcRatio: Option[Output] = {
      for {
        taskTime <- valuesMap.get(Metrics.TotalTaskTime)
        gcTime   <- valuesMap.get(Metrics.TotalGCTime)
      } yield {
        Output.FractionalMetric("GCRatio", gcTime.toDouble / taskTime)
      }
    }

    val effectiveParallelism: Option[Output] = {
      for {
        taskTime <- valuesMap.get(Metrics.TotalTaskTime)
        duration <- valuesMap.get(Metrics.Duration)
      } yield {
        Output.FractionalMetric("EffectiveParallelism", taskTime.toDouble / duration)
      }
    }

    List(
      gcRatio,
      effectiveParallelism,
    )
      .flatten
  }
}
