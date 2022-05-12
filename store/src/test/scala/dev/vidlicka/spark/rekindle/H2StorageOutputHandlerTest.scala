package dev.vidlicka.spark.rekindle.jdbc

import cats.effect.*
import cats.implicits.*
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.util.*
import fs2.*
import fs2.io.file.Path
import weaver.*

import dev.vidlicka.spark.rekindle.*
import dev.vidlicka.spark.rekindle.replayers.*

// TODO a lot of copying of EngineSmokeTest
object H2StorageOutputHandlerTest extends SimpleIOSuite {
  val Driver           = "org.h2.Driver"
  val ConnectionString = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
  val User             = ""
  val Password         = ""

  val eventLogPath = getClass().getResource("/event-logs/simple.log").getPath

  test("smoke test") {
    val eventLog: Stream[IO, String] = FileEventLogSource.lineStream[IO](Path(eventLogPath), false)

    val outputStream = {
      RekindleEngine.process[IO](
        Replayers.combine(
          canonicalReplayers *,
        ),
        EventLogMetadata("smoke-test"),
        eventLog,
      )
    }

    (
      for {
        ce <- ExecutionContexts.fixedThreadPool[IO](32) // our connect EC
        xa <- HikariTransactor.newHikariTransactor[IO](
          Driver,
          ConnectionString,
          User,
          Password,
          ce,
        )
      } yield xa
    ).use { xa =>
      for {
        store        <- jdbcStorage[IO](100, xa)
        _            <- createTables(xa)
        _            <- outputStream.through(store).compile.drain
        countMetrics <- sql"select count(*) from metrics".query[Int].unique.transact(xa)
        countMetricsIndentifiers <-
          sql"select count(*) from observations where kind = 'metric'".query[Int].unique.transact(
            xa,
          )
      } yield {
        expect(countMetrics >= countMetricsIndentifiers)
      }
    }
  }

  private def createTables(xa: Transactor[IO]): IO[Unit] = {
    sql"""
CREATE TABLE application_infos(
    id BIGSERIAL NOT NULL PRIMARY KEY,
    application_id VARCHAR(255) NOT NULL,
    attempt_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    start BIGINT NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    UNIQUE(application_id, attempt_id, name, identifier)
);
CREATE TABLE observations(
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    kind VARCHAR(255) NOT NULL,
    UNIQUE(name)
);
CREATE TABLE metrics(
    app_id BIGINT NOT NULL,
    observation_id INT NOT NULL,
    metric_value BIGINT NOT NULL,
    PRIMARY KEY (app_id, observation_id),
    FOREIGN KEY (app_id) REFERENCES application_infos(id) ON DELETE CASCADE,
    FOREIGN KEY (observation_id) REFERENCES observations(id) ON DELETE CASCADE
);
CREATE TABLE indexed_metrics(
    app_id BIGINT NOT NULL,
    observation_id INT NOT NULL,
    index INT NOT NULL,
    metric_value BIGINT NOT NULL,
    PRIMARY KEY (app_id, observation_id, index),
    FOREIGN KEY (app_id) REFERENCES application_infos(id) ON DELETE CASCADE,
    FOREIGN KEY (observation_id) REFERENCES observations(id) ON DELETE CASCADE
);
CREATE TABLE messages(
    app_id BIGINT NOT NULL,
    observation_id INT NOT NULL,
    contents TEXT NOT NULL,
    PRIMARY KEY (app_id, observation_id),
    FOREIGN KEY (app_id) REFERENCES application_infos(id) ON DELETE CASCADE,
    FOREIGN KEY (observation_id) REFERENCES observations(id) ON DELETE CASCADE
);
""".update.run.transact(xa).void
  }
}
