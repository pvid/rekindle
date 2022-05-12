package dev.vidlicka.spark.rekindle.replayers

import dev.vidlicka.spark.rekindle.Replayer

def canonicalReplayers[F[_]]: List[Replayer[F]] = {
  List(
    LogSizeReplayer(),
    SimpleSummaryReplayer(),
    StageDurationReplayer(),
    SparkVersionReplayer(),
  )
}
