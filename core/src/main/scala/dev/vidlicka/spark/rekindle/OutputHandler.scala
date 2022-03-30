package dev.vidlicka.spark.rekindle

import fs2.{ Pipe, Stream }

/** this handles pushing the results of replayers somewhere idea:
  *   - print to stdout
  *   - store in a datastore
  *   - push to Slack / send emails
  *   - emit to Kafka
  */
type OutputHandler[F[_]] = Pipe[F, (ApplicationInfo, Stream[F, Observation]), Unit]
