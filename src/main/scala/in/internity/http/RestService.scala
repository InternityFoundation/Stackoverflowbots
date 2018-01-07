package in.internity.http


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import in.internity.TimeCache
import in.internity.config.AppConfig
import in.internity.datasource.SaveConfigurationsDB
import in.internity.models.TwitterApi
import in.internity.stackoverflow.{Fetch, QuestionsActor}
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
          val listOfRunningBots = SaveConfigurationsDB.getAll().map { a =>
            s"""${a.tag} --> <a href="https://twitter.com/${a.twitterApi.handler}">${a.twitterApi.handler}</a>"""
          }.mkString("</br>")
          val updated = html.split("</body>").mkString(listOfRunningBots + "</body>")
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
              val twitterHandler = new TwitterCommunicator(twitterApi,Http())
              val questionsActor = actorSystem.actorOf(QuestionsActor.props(AppConfig.questionsURL, AppConfig.authKey, twitterHandler))
              val latestTimeStamp = TimeCache.getLatestTime(tag)
              SaveConfigurationsDB.save(twitterApi, tag, latestTimeStamp.toLong)
              actorSystem.scheduler.schedule(500 millis, 1 minute) {
                questionsActor ! Fetch(tag, latestTimeStamp)
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

  def stop(bindingFuture: Future[ServerBinding])(implicit actorSystem: ActorSystem): Future[Unit] = {
    implicit val executionContext = actorSystem.dispatcher

    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
  }
}
