package benchmark

import benchmark.Memory.MemoryUsage
import benchmark.Reporter.Event
import play.api.mvc.Request

import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.util.{Success, Try}


trait Reporter {

  def report(event: Event): Unit

}

object Memory {


  val KB = 1024
  val MB = KB * 1024

  case class MemoryUsage(total: Long, used: Long, free: Long, max: Long)

  def toKB(bytes: Long): Long = bytes / KB

  def toMB(bytes: Long): Long = bytes / MB

  def diff(m1: MemoryUsage, m2: MemoryUsage): Long =
    m2.used - m1.used


  def measure: MemoryUsage = {
    val rt = Runtime.getRuntime
    val total = rt.totalMemory()
    val free = rt.freeMemory()
    val used = rt.totalMemory() - rt.freeMemory()
    val max = rt.maxMemory()

    MemoryUsage(total, used, free, max)
  }
}


object Reporter {

  type Result = Try[Unit]

  val empty = new Reporter {
    override def report(event: Event): Unit = ()
  }

  def event[T <: Event](evt: StartEvent => T)(implicit request: Request[_]): T =
    evt(StartEvent(request.id, System.currentTimeMillis(), Memory.measure))

  def event[T <: Event](evt: FinishedEvent => T, result: Result = Success(()))(implicit request: Request[_]): T =
    evt(FinishedEvent(StartEvent(request.id, System.currentTimeMillis(), Memory.measure), result))

  def reportFuture[F](fut: => Future[F])(reporter: Reporter, started: StartEvent => Event, finished: FinishedEvent => Event)(implicit request: Request[_], ec: ExecutionContext): Future[F] = {
    reporter.report(Reporter.event(started))

    val result = fut

    Reporter.futureEvent(finished)(result).map(reporter.report)

    result
  }

  def futureEvent[T <: Event](evt: FinishedEvent => T)(fut: Future[_])(implicit request: Request[_], ec: ExecutionContext): Future[T] = {
    val p = Promise[T]()

    fut.onComplete(res => {
      val result: Result = res.map(_ => ())
      val newEvent = event(evt, result)

      p.success(newEvent)
    })

    p.future
  }

  trait BenchmarkedEvent {
    def id: Long
    def time: Long
    def memory: MemoryUsage
  }

  case class StartEvent(id: Long, time: Long, memory: MemoryUsage) extends BenchmarkedEvent
  
  case class FinishedEvent(event: StartEvent, result: Result) extends BenchmarkedEvent {
    def id = event.id
    def time = event.time
    def memory = event.memory
  }

  sealed trait Event {
    val event: BenchmarkedEvent
  }

  case class EngineStart(event: StartEvent) extends Event

  case class RequestStart(event: StartEvent)  extends Event

  case class EngineFinished(event: FinishedEvent) extends Event

  case class RequestFinished(event: FinishedEvent) extends Event

}