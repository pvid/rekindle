package dev.vidlicka.spark.rekindle

import cats.*
import cats.arrow.FunctionK
import cats.effect.Concurrent
import cats.implicits.*
import fs2.{ Compiler, Stream }
import io.chrisdavenport.mapref.MapRef

import dev.vidlicka.spark.rekindle.Observation.*
import dev.vidlicka.spark.rekindle.RepositoryBasedStore.*

trait ApplicationInfoRepository[F[_]] {
  def inserOrGetId(applicationInfo: ApplicationInfo): F[Long]
}

trait ObservationIdentifierRepository[F[_]] {
  def insertOrGetId(identifier: ObservationIdentifier): F[Int]
}

object ObservationIdentifierRepository {
  def cached[F[_]: Concurrent](
      underlying: ObservationIdentifierRepository[F],
  ): F[ObservationIdentifierRepository[F]] = {
    for {
      cache <- MapRef.ofConcurrentHashMap[F, String, Int]()
    } yield {
      Cached(underlying, cache)
    }
  }

  private class Cached[F[_]: Monad](
      underlying: ObservationIdentifierRepository[F],
      cache: MapRef[F, String, Option[Int]],
  ) extends ObservationIdentifierRepository[F] {
    def insertOrGetId(identifier: ObservationIdentifier): F[Int] = {
      cache
        .apply(identifier.name)
        .access
        .flatMap { case (cachedValue, setter) =>
          cachedValue
            .fold {
              for {
                id <- underlying.insertOrGetId(identifier)
                // if setter returns false, that just means that some concurrent
                // action populated the cache
                _ <- setter(Some(id))
              } yield id
            } {
              _.pure[F]
            }
        }
    }
  }
}

trait CompactObservationRepository[F[_]] {
  def insertMetrics(metrics: Seq[CompactMetric]): F[Unit]
  def insertIndexedMetrics(metrics: Seq[CompactIndexedMetric]): F[Unit]
  def insertMessages(messages: Seq[CompactMessage]): F[Unit]
}

class StorageOutputHandler[F[_]: Concurrent](
    appInfoRepository: ApplicationInfoRepository[F],
    observationIdRepository: ObservationIdentifierRepository[F],
    observationValuesRepository: CompactObservationRepository[F],
    batchSize: Int,
) extends OutputHandler[F] {
  def apply(output: Stream[F, (ApplicationInfo, Stream[F, Observation])]): Stream[F, Unit] = {
    output
      .evalMap(insertAppObservations)
  }

  private def insertAppObservations(
      appInfo: ApplicationInfo,
      data: Stream[F, Observation],
  ): F[Unit] = {
    for {
      appId <- appInfoRepository.inserOrGetId(appInfo)
      _     <- insertObservations(appId, data)
    } yield {}
  }

  private def insertObservations(appId: Long, data: Stream[F, Observation]): F[Unit] = {
    data
      .chunkN(batchSize, allowFewer = true)
      .evalMap { chunk =>
        chunk
          .toArraySeq
          .groupBy(ObservationKind.forObservation)
          .map {
            case (ObservationKind.Metric, observations) => {
              observations
                .collect {
                  case m: Metric => {
                    observationIdRepository
                      .insertOrGetId(ObservationIdentifier(m.name, ObservationKind.Metric))
                      .map { id => CompactMetric(appId, id, m.value) }
                  }
                }
                .sequence
                .flatMap(observationValuesRepository.insertMetrics)
            }

            case (ObservationKind.IndexedMetric, observations) => {
              observations
                .collect {
                  case m: IndexedMetric => {
                    observationIdRepository
                      .insertOrGetId(ObservationIdentifier(m.name, ObservationKind.IndexedMetric))
                      .map { id => CompactIndexedMetric(appId, id, m.index, m.value) }
                  }
                }
                .sequence
                .flatMap(observationValuesRepository.insertIndexedMetrics)
            }

            case (ObservationKind.Message, observations) => {
              observations
                .collect {
                  case m: Message => {
                    observationIdRepository
                      .insertOrGetId(ObservationIdentifier(m.name, ObservationKind.Message))
                      .map { id => CompactMessage(appId, id, m.contents) }
                  }
                }
                .sequence
                .flatMap(observationValuesRepository.insertMessages)
            }
          }
          .toList
          .sequence
          .void
      }
      .compile
      .drain
  }
}

object RepositoryBasedStore {
  opaque type AppId         = Long
  opaque type ObservationId = Int
}
