package dev.vidlicka.spark.rekindle

import cats.effect.Concurrent
import cats.implicits.*
import fs2.Stream
import weaver.Expectations
import weaver.Expectations.Helpers.*

object TestHelpers {
  def collectOutputs[F[_]: Concurrent](
      outputs: Stream[F, (ApplicationInfo, Stream[F, Output])],
  ): F[List[(ApplicationInfo, List[Output])]] = {
    outputs
      .evalMap { case (appInfo, outputs) => outputs.compile.toList.map((appInfo, _)) }
      .compile
      .toList
  }

  def withCollectedOutputs[F[_]: Concurrent](
      outputs: Stream[F, (ApplicationInfo, Stream[F, Output])],
  )(
      test: List[(ApplicationInfo, List[Output])] => Expectations,
  ): F[Expectations] = {
    collectOutputs(outputs).map(test)
  }

  def withCollectedOutputsForSingleApp[F[_]: Concurrent](
      outputs: Stream[F, (ApplicationInfo, Stream[F, Output])],
  )(
      test: ((ApplicationInfo, List[Output])) => Expectations,
  ): F[Expectations] = {
    collectOutputs(outputs)
      .map {
        case Nil         => failure("Output is empty!")
        case head :: Nil => test(head)
        case _           => failure("Stream contains outputs for more than a single event log!")
      }
  }
}
