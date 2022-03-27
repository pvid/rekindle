package dev.vidlicka.spark.rekindle

import cats.Traverse

import cats.Id
import java.util.concurrent.ConcurrentMap
import scala.collection.mutable
import io.chrisdavenport.mapref.MapRef

// TODO the state is not correct. It should be more or less Map[ApplicationInfo, Map[OutputName, Output]]
// I am thinking that it should be split into multiple tables, one for applications, the other with outputs
// this split could be relatively generic and shared between store implementations. There could be a more
// low-level Interface that would be responsible for storing individual logical tables
// NOTE: What should have a separate tables:
// - application info - table with a in-memory cache (at least for ingestor) (with time based expiration)
// - output names - primary key would be an int. We do not have to store name next to each output
//                  there should be a in-memory cache, without expiration and with size > number of metrics
//                  it should be configuratble and issue a warning if values are removed from it
class InMemoryStore[F[_]](state: MapRef[F, ApplicationInfo, List[Output]]) extends Store[Id] {
  opaque type Id = ApplicationInfo


  // Ingest
  def writeBatch[G[_]: Traverse](appInfo: ApplicationInfo, data: G[Output]): Unit = {
    ???
  }

  // Query
  // TODO filtering - there could be some filter abstraction for app filters
  //                  outputs can by filtered by a combination of app filter + output name filter
  def listAppInfo(): List[(Id, ApplicationInfo)] = {
    ???
  }
  def outputsForApp(id: Id): List[Output] = {
    ???
  }
}
