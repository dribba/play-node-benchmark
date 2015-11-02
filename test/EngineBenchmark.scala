import java.util.concurrent.TimeUnit
import javax.script.{ScriptEngine, ScriptEngineManager}

import akka.actor.ActorSystem
import akka.util.Timeout
import benchmark.Reporter
import benchmark.Reporter.{TestFinished, TestStart, RequestFinished, RequestStart}
import controllers.React
import play.api.mvc._
import play.api.test.{FakeHeaders, PlaySpecification, FakeApplication, FakeRequest}

import scala.concurrent.Future
import scala.util.{Success, Failure}

class EngineBenchmark extends PlaySpecification {


  def timing[T](reporter: Reporter)(block: Request[AnyContent] => Future[T])(implicit request: Request[AnyContent]): Future[T] = {
    Reporter.reportFuture(block(request))(reporter, RequestStart, RequestFinished)
  }

  val reportSystem = ActorSystem("Reports")

  def Aggregator(name: String, runs: Int) = new ReportAggregator(reportSystem)(name, runs)

  def request(id: Long) = FakeRequest("", "", FakeHeaders(), AnyContentAsEmpty, id=id)

  def test[T, R](name: String, warmup: Boolean = true)(block: Reporter => Request[AnyContent] => Future[T])(runs: Int): Unit = {
    // Warmup
    if (warmup) {
      println("Starting warmup for: " + name)
      await(Future.sequence(for {
        i <- 0 until 20
        req = request((runs * 2) + i.toLong) // avoid colliding request ids
      } yield timing(Reporter.empty)(block(Reporter.empty))(req)), 5, TimeUnit.MINUTES)

      println("Finished warmup for: " + name)
    }

    // Cleanup
    Runtime.getRuntime.gc()

    val reporter = Aggregator(name, runs)

    val testReq = request(0)
    reporter.report(Reporter.event(TestStart)(testReq))

    val results = for {
      i <- 0 until runs
      req = request(i.toLong)
    } yield timing(reporter)(block(reporter))(req)

    await(Future.sequence(results), 1, TimeUnit.HOURS)

    reporter.report(Reporter.event(TestFinished)(testReq))

    await(reporter.printReport.map(println), 5, TimeUnit.MINUTES)
  }


  val Single = 1
  val Cores = 7
  val StressL0 = 50
  val StressL1 = 250
  val StressL2 = 1000
  val StressL3 = 10000

  def cleanup(): Unit = {
    Runtime.getRuntime.gc()
    Thread.sleep(500)
  }


  implicit def toFut(action: Action[AnyContent]): Request[AnyContent] => Future[Result] = req => {
    action(req)
  }

  "test" in {
    def withEngine(block: ScriptEngine => Int => Unit) =
      block(new ScriptEngineManager(null).getEngineByName("nashorn"))

    def withFactory(block: ScriptEngineManager => Int => Unit) = block(new ScriptEngineManager(null))

    def run(theTest: Int => Unit)(runs: Int*): Unit = {
      for(times <- runs) {
        cleanup()
        theTest(times)
      }
    }


    running(FakeApplication()) {

//      run(test("Client")(React.clientSide))(StressL2)
//
//      run(test("Nashorn new engine")(React.nashorn))(Single, Cores, StressL0)
//
//      run {
//        withFactory(factory => {
//          test("Nashorn same factory")(rep => req => React.nas(factory.getEngineByName("nashorn"))(rep)(req))
//        })
//      }(Single, Cores, StressL0)
//
//      run {
//        withEngine(engine => {
//          test("Nashorn same engine")(React.nas(engine)(_))
//        })
//      }(Single, Cores, StressL0, StressL1, StressL2, StressL3)

      run(test("Node")(React.serverSide))(Single, Cores, StressL0, StressL1, StressL2)
    }
    true
  }


}
