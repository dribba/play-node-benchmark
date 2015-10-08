import Aggregations.Aggregation
import akka.actor.{PoisonPill, Props, ActorSystem, Actor}
import akka.util.Timeout
import benchmark.{Memory, Reporter}
import benchmark.Reporter._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.ask
import Memory._
import scala.collection.mutable

import scala.util.Try
import scala.concurrent.duration._


class DataActor(size: Int) extends Actor {

  val data = mutable.Map.empty[Class[_ <: Event], Array[BenchmarkedEvent]]

  def aggregate(startEvents: Array[BenchmarkedEvent], finishEvents: Array[BenchmarkedEvent]): Array[PartialAggregation] = {
    Array((for {
      i <- 0 until size
      StartEvent(_, sTime, sMem) = startEvents(i)
      FinishedEvent(StartEvent(id, eTime, eMem), result) = finishEvents(i)
    } yield {
      val timeD = eTime - sTime
      val memD = Memory.diff(sMem, eMem)

      PartialAggregation(id, timeD, memD, result)
    }):_*)
  }

  def defaultArray: Array[BenchmarkedEvent] =
    Array.fill(size)(null)

  override def receive = {
    case NewEvent(e) =>
      val key = e.getClass
      data.getOrElseUpdate(key, defaultArray).update(e.event.id.toInt, e.event)

    case PartialAggregatedData =>
      val aggregated = Aggregations.available.flatMap(agg => {
        for {
          start <- data.get(agg.evts._1)
          end <- data.get(agg.evts._2)
        } yield agg -> aggregate(start, end)
      }).toMap
      self ! PoisonPill
      sender ! EventsData(aggregated)
  }

}

case class NewEvent(e: Event)

case class EventsData(data: Map[Aggregation, Array[PartialAggregation]])

case object PartialAggregatedData


object Aggregations {

  sealed abstract class Aggregation(val evts: (Class[_ <: Event], Class[_ <: Event])) {
    val name: String
  }

  object RequestResult extends Aggregation(classOf[RequestStart] -> classOf[RequestFinished]) {
    val name = "Request"
  }

  object EngineResult extends Aggregation(classOf[EngineStart] -> classOf[EngineFinished]) {
    val name = "Engine"
  }

  val available = List(RequestResult, EngineResult)

  def apply(evt: Class[_ <: Event]): Aggregation =
    available.find(a => a.evts._1 == evt || a.evts._2 == evt).get

}

case class PartialAggregation(id: Long, timeDelta: Long, memoryDelta: Long, result: Result)

case class AggregatedResult(processed: Int, time: Long, memory: Long, success: Int) {

  def avgTime = Try(time / processed).getOrElse(0L)

  def avgMem = Try(memory / processed).getOrElse(0L)

  def successPer = (success.toDouble * 100) / processed


  def add(partial: PartialAggregation): AggregatedResult = {
    val succ = if(partial.result.isSuccess) {
      success + 1
    } else {
      success
    }

    copy(processed + 1, time + partial.timeDelta, memory + partial.memoryDelta, succ)
  }

}


class ReportAggregator(system: ActorSystem)(name: String, testSize: Int) extends Reporter {


  val EmptyResult = AggregatedResult(0, 0, 0, 0)

  val dataActor = system.actorOf(Props(classOf[DataActor], testSize), "reporter-for-" + name.replace(" ", "-"))
  implicit val timeout = Timeout(5 seconds)


  override def report(event: Event): Unit =
    dataActor ! NewEvent(event)


  private def accumulator(result: AggregatedResult, partial: PartialAggregation): AggregatedResult =
    result add partial

  private def aggregateResults(partials: Array[PartialAggregation]): AggregatedResult =
    partials.foldLeft(EmptyResult)(accumulator)

  def printReport: Future[String] = {
    (dataActor ? PartialAggregatedData).map {
      case EventsData(data) =>
        data.map { case (agg, res) =>
          agg.name -> aggregateResults(res)
        }
    }.map(result => {
      def toStringTime(r: AggregatedResult): String =
        s"${r.time}ms (avg ${r.avgTime}ms)"

      def toStringMem(r: AggregatedResult): String =
        s"${r.memory} bytes (${toKB(r.memory)} KB) (${toMB(r.memory)} MB) AVG.: ${r.avgMem} bytes (${toKB(r.avgMem)} KB) (${toMB(r.avgMem)} MB)"

      def toStringSuccess(r: AggregatedResult): String =
        s"${r.success} (${r.successPer}%)"

      val toString = result.map { case (aggregationName, aggregationResults) =>
          s"""
             | Running: $aggregationName
             |    Totals:
             |      runs: ${aggregationResults.processed}
             |      time: ${toStringTime(aggregationResults)}
             |      memory: ${toStringMem(aggregationResults)}
             |      success: ${toStringSuccess(aggregationResults)}
           """.stripMargin
      }.mkString("\n")

      s"""
         |
         | Ran test '$name':
         |
         |  $toString
         |
      """.stripMargin
    })
  }

}
