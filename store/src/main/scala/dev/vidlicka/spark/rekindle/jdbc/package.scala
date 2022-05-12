package dev.vidlicka.spark.rekindle.jdbc

import cats.*
import cats.effect.*
import cats.effect.implicits.*
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.util.*

import dev.vidlicka.spark.rekindle.*

// Nothing runs in a transaction

def jdbcStorage[F[_]: Async](
    batchSize: Int,
    xa: Transactor[F],
): F[StorageOutputHandler[F]] = {
  ObservationIdentifierRepository
    .cached(DoobieObservationIdentifierRepository(xa))
    .map {
      StorageOutputHandler(
        DoobieApplicationInfoRepository(xa),
        _,
        DoobieCompactObservationRepository(xa),
        batchSize,
      )
    }
}

class DoobieApplicationInfoRepository[F[_]: Sync](xa: Transactor[F])
    extends ApplicationInfoRepository[F] {
  def inserOrGetId(appInfo: ApplicationInfo): F[Long] = {
    sql"""INSERT INTO
         |  application_infos (application_id, attempt_id, name, start, identifier)
         |VALUES
         |  (
         |    ${appInfo.applicationId},
         |    ${appInfo.attemptId},
         |    ${appInfo.name},
         |    ${appInfo.start},
         |    ${appInfo.eventLogMetadata.identifier}
         |   )"""
      .stripMargin
      .update
      .withUniqueGeneratedKeys[Long]("id")
      .exceptSql { _ =>
        sql"""SELECT id FROM application_infos WHERE
             |application_id = ${appInfo.applicationId} AND
             |attempt_id = ${appInfo.attemptId} AND
             |name = ${appInfo.name} AND
             |identifier = ${appInfo.eventLogMetadata.identifier}"""
          .stripMargin
          .query[Long]
          .unique
      }
      .transact(xa)
  }
}

class DoobieObservationIdentifierRepository[F[_]: Sync](xa: Transactor[F])
    extends ObservationIdentifierRepository[F] {
  def insertOrGetId(identifier: ObservationIdentifier): F[Int] = {
    sql"""INSERT INTO
         |  observations (name, kind)
         |VALUES
         |  (
         |    ${identifier.name},
         |    ${identifier.kind.asString}
         |   )"""
      .stripMargin
      .update
      .withUniqueGeneratedKeys[Int]("id")
      .exceptSql { _ =>
        sql"SELECT id FROM observations WHERE name = ${identifier.name}"
          .query[Int]
          .unique
      }
      .transact(xa)
  }
}

class DoobieCompactObservationRepository[F[_]: Sync](xa: Transactor[F])
    extends CompactObservationRepository[F] {
  val metricsInsert = Update[CompactMetric](
    "insert into metrics (app_id, observation_id, metric_value) values (?, ?, ?)",
  )

  val indexedMetricsInsert = Update[CompactIndexedMetric](
    "insert into indexed_metrics (app_id, observation_id, index, metric_value) values (?, ?, ?, ?)",
  )

  val messageInsert = Update[CompactMessage](
    "insert into messages (app_id, observation_id, contents) values (?, ? ,?)",
  )

  def insertMetrics(metrics: Seq[CompactMetric]): F[Unit] = {
    metricsInsert
      .updateMany(metrics)
      .void
      .transact(xa)
  }

  def insertIndexedMetrics(metrics: Seq[CompactIndexedMetric]): F[Unit] = {
    indexedMetricsInsert
      .updateMany(metrics)
      .void
      .transact(xa)
  }

  def insertMessages(messages: Seq[CompactMessage]): F[Unit] = {
    messageInsert
      .updateMany(messages)
      .void
      .transact(xa)
  }
}
