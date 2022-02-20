package dev.vidlicka.spark.rekindle

import fs2.Stream

/** this handles pushing the results of replayers somewhere idea:
  *   - print to stdout
  *   - store in a datastore
  *   - push to Slack / send emails
  *   - emit to Kafka
  */
trait OutputHandler[F[_]] {
  def apply(outputStream: Stream[F, (ApplicationInfo, Output)]): Stream[F, Unit]
}
