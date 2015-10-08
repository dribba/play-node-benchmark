package controllers

import java.io._
import javax.script.{ScriptEngine, SimpleScriptContext, ScriptEngineManager}

import akka.util.Timeout
import benchmark.Reporter
import benchmark.Reporter.{EngineFinished, RequestFinished, EngineStart, RequestStart}
import com.typesafe.jse.{Node, Engine}
import com.typesafe.jse.Engine.JsExecutionResult
import concurrent.Pools
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.api.{Logger, Play}
import play.twirl.api.Html


import scala.concurrent.Future
import scala.concurrent.duration._

object React extends Controller {

  import play.api.Play.current
  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  import scala.language.postfixOps

  val serversideFile = "app/assets/javascripts/react/serverside.js"

  val json = Json.stringify(Json.parse(
    """
      |{
      | "email": "john@example.com",
      | "type": "foo",
      | "test": "bar"
      | }
    """.stripMargin
  ))


  def serverSide(reporter: Reporter = Reporter.empty) = Action.async { implicit request =>
    import akka.pattern.ask

    val serverSide = Play.getFile(serversideFile)
    implicit val timeout = Timeout(10.minutes)
    val engine = Akka.system.actorOf(Node.props(), s"engine-${request.id}")

    reporter.report(Reporter.event(EngineStart))
    for {
      result <- (engine ? Engine.ExecuteJs(
        source = new File(serverSide.toURI),
        args = List(json),
        timeout = timeout.duration
      )).mapTo[JsExecutionResult]
    } yield {
        reporter.report(Reporter.event(EngineFinished))
        val error = new String(result.error.toArray, "UTF-8")
        if (!error.isEmpty) Logger.error(error)

        val output = new String(result.output.toArray, "UTF-8")
        Ok(views.html.index(Html(output)))
      }
  }

  def clientSide(reporter: Reporter = Reporter.empty) = Action.async { implicit req =>
    def result = Future.successful(Ok(views.html.index()))
    Reporter.reportFuture(result)(reporter, EngineStart, EngineFinished)
  }

  def data = Action(Ok(json))

  def nas(engine: ScriptEngine)(reporter: Reporter = Reporter.empty) = Action.async { implicit req =>
      Future {
        if (engine == null) {
          BadRequest("Nashorn script engine not found. Are you using JDK 8?")
        } else {

          val sw = new StringWriter()
          val ctx = new SimpleScriptContext()

          ctx.setWriter(sw)

          reporter.report(Reporter.event(EngineStart))
          engine.eval(s"data = JSON.parse('$json');", ctx)
          engine.eval(new FileReader("app/assets/javascripts/react/public/output-nashorn.js"), ctx)
          reporter.report(Reporter.event(EngineFinished))

          Ok(views.html.index(Html(sw.toString)))
        }
      }(Pools.jsPool)
  }

  def nashorn(reporter: Reporter = Reporter.empty) =
    nas(new ScriptEngineManager(null).getEngineByName("nashorn"))(reporter)


}
