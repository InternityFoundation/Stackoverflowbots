package in.internity.http


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import in.internity.Boot.actorSystem
import in.internity.TimeCache
import in.internity.config.AppConfig
import in.internity.datasource.SaveConfigurationsDB
import in.internity.models.TwitterApi
import in.internity.stackoverflow.{CallHeroku, Fetch, QuestionsActor}
import in.internity.twitter.TwitterCommunicator
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, native}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.xml.Elem

/**
  * @author Shivansh <shiv4nsh@gmail.com>
  * @since 7/1/18
  */


object RestService {

  val config = AppConfig
  val html = Source.fromFile(config.fileAddress).mkString

  import Json4sSupport._

  implicit val formats: DefaultFormats.type = DefaultFormats
  implicit val serialization: Serialization.type = native.Serialization

  def run()(implicit actorSystem: ActorSystem, actorMaterializer: ActorMaterializer): Future[ServerBinding] = {

    val routes =
      path("") {
        get {
          val updated: String = getHtmlToBeRendered
          val resp: Elem = scala.xml.XML.loadString(updated)
          complete(resp)
        }
      } ~ path("heartbeat") {
        get {
          complete("I am alive ! ! Heahahaha :D")
        }
      } ~ path("createBot" / Segment) { tag: String =>
        post {
          decodeRequest {
            entity(as[TwitterApi]) { twitterApi: TwitterApi =>
              val twitterHandler = new TwitterCommunicator(twitterApi, Http())
              val questionsActor = actorSystem.actorOf(QuestionsActor.props(AppConfig.questionsURL, AppConfig.authKey, twitterHandler))
              val latestTimeStamp = TimeCache.getLatestTime(tag)
              SaveConfigurationsDB.save(twitterApi, tag, latestTimeStamp.toLong,twitterApi.herokuURL)
              actorSystem.scheduler.schedule(500 millis, 1 minute) {
                questionsActor ! Fetch(tag, latestTimeStamp)
              }
              actorSystem.scheduler.schedule(500 millis,10 minute) {
                questionsActor ! CallHeroku(twitterApi.herokuURL)
              }
              complete {
                "Handler Added"
              }
            }
          }
        }
      }

    Http().bindAndHandle(routes, config.address, config.port)
  }

  private def getHtmlToBeRendered = {
    val listOfRunningBots = SaveConfigurationsDB.getAll().map { a =>
      s"""<h4 align="center" style="margin-top:5%;font-weight:400">${a.tag}:<a href="https://twitter.com/${a.twitterApi.handler}">${a.twitterApi.handler}</a></h4>"""
    }.mkString("\n")
    val updated = html.split("</div>").mkString(listOfRunningBots +
      """
        |<h5 align="center" style="margin-top:10%;font-weight:400">Maintained By: <a href="https://www.twitter.com/Internity_learn">InternityFoundation</a></h5>
      """.stripMargin +
      "\n </div>")
    updated
  }

  def stop(bindingFuture: Future[ServerBinding])(implicit actorSystem: ActorSystem): Future[Unit] = {
    implicit val executionContext = actorSystem.dispatcher

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
  }
}
