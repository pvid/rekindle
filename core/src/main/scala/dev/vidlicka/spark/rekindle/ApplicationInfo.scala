package dev.vidlicka.spark.rekindle

import io.circe.Codec

final case class ApplicationInfo(
    applicationId: String,
    attemptId: String,
    name: String,
    start: Long,
    eventLogMetadata: EventLogMetadata,
) derives Codec.AsObject
