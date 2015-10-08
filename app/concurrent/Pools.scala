package concurrent

import play.libs.Akka

import scala.concurrent.ExecutionContext

object Pools {

  implicit val jsPool: ExecutionContext = Akka.system.dispatchers.lookup("play.js-pool")
}
