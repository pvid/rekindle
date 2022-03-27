package dev.vidlicka.spark.rekindle

import cats.Traverse

/** Generic store for rekindke output
 *
 * Very much WIP. It may be split into two interface, one for ingest and one for querying
 * It may well be that the ingest par may be handled by the [[OutputHandler]] trait or similar
*/
trait Store[F[_]] {
  // Id used to identify a single Spark application
  // not to be confused with applicationId
  type Id

  // Ingest
  def write(appInfo: ApplicationInfo, data: Output): F[Unit] = {
    writeBatch(appInfo, List(data))
  }

  def writeBatch[G[_]: Traverse](appInfo: ApplicationInfo, data: G[Output]): F[Unit]

  // Query
  // TODO filtering - there could be some filter abstraction for app filters
  //                  outputs can by filtered by a combination of app filter + output name filter
  def listAppInfo(): F[List[(Id, ApplicationInfo)]]
  def outputsForApp(id: Id): F[List[Output]]
}
