package dev.vidlicka.spark.rekindle

final case class ApplicationInfo(
    applicationId: String,
    attemptId: String,
    name: String,
    start: Long,
    eventLogMetadata: EventLogMetadata,
)
