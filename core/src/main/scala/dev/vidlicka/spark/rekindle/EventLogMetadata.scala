package dev.vidlicka.spark.rekindle

import io.circe.Codec

/** Metadata describing provenance of an event log */
final case class EventLogMetadata(identifier: String) derives Codec.AsObject
