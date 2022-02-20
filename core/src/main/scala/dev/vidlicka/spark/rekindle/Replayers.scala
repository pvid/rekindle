package dev.vidlicka.spark.rekindle

import cats.effect.Concurrent

object Replayers {
  def combine[F[_]: Concurrent](replayers: Replayer[F]*): Replayer[F] = { events =>
    events.broadcastThrough(replayers*)
  }
}
