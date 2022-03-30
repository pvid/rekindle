package dev.vidlicka.spark.rekindle

import cats.effect.Concurrent
import cats.implicits.*
import fs2.Stream
import weaver.Expectations
import weaver.Expectations.Helpers.*

object TestHelpers {
  def collectObservations[F[_]: Concurrent](
      outputs: Stream[F, (ApplicationInfo, Stream[F, Observation])],
  ): F[List[(ApplicationInfo, List[Observation])]] = {
    outputs
      .evalMap { case (appInfo, outputs) => outputs.compile.toList.map((appInfo, _)) }
      .compile
      .toList
  }

  def withCollectedObservations[F[_]: Concurrent](
      outputs: Stream[F, (ApplicationInfo, Stream[F, Observation])],
  )(
      test: List[(ApplicationInfo, List[Observation])] => Expectations,
  ): F[Expectations] = {
    collectObservations(outputs).map(test)
  }

  def withCollectedObservationsForSingleApp[F[_]: Concurrent](
      outputs: Stream[F, (ApplicationInfo, Stream[F, Observation])],
  )(
      test: ((ApplicationInfo, List[Observation])) => Expectations,
  ): F[Expectations] = {
    collectObservations(outputs)
      .map {
        case Nil         => failure("Observation is empty!")
        case head :: Nil => test(head)
        case _           => failure("Stream contains outputs for more than a single event log!")
      }
  }
}
