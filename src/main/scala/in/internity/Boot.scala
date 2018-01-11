package in.internity

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import in.internity.datasource.SaveConfigurationsDB
import in.internity.http.RestService
import in.internity.stackoverflow.{CallHeroku, Fetch, QuestionsActor}
import in.internity.twitter.TwitterCommunicator
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

/**
  * @author Shivansh <shiv4nsh@gmail.com>
  * @since 6/1/18
  */
object Boot extends App {

  import in.internity.config.AppConfig._

  implicit val actorSystem: ActorSystem = ActorSystem("StackoverflowTwitterBots")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

  val bind = RestService.run()

  bind.onComplete(a => println(a.get))
  SaveConfigurationsDB.init()
  SaveConfigurationsDB.getAll().map { conf =>
    val twitterCommunicator = new TwitterCommunicator(conf.twitterApi,Http())
    val questionsActor = actorSystem.actorOf(QuestionsActor.props(questionsURL, authKey, twitterCommunicator))
    val latestTS = Math.max(TimeCache.getLatestTime(conf.tag), conf.lastTimestamp)
    TimeCache.updateTimeAndTag(conf.tag, latestTS)
    actorSystem.scheduler.schedule(500 millis,1 minute) {
      questionsActor ! Fetch(conf.tag, TimeCache.getLatestTime(conf.tag))
    }
    actorSystem.scheduler.schedule(500 millis,10 minute) {
      questionsActor ! CallHeroku(conf.herokuURL)
    }
  }
}

object TimeCache {
  private val tagAndTime = mutable.Map[String, Double]()

  def getLatestTime(tag: String): Double = tagAndTime.getOrElse(tag, new DateTime(DateTimeZone.UTC).withTimeAtStartOfDay().getMillis / 1000)

  def updateTimeAndTag(tag: String, time: Double) = tagAndTime.update(tag, time)
}