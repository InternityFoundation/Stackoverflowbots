package in.internity

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import in.internity.datasource.SaveConfigurationsDB
import in.internity.http.RestService
import in.internity.slack.SlackCommunicator
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
  SaveConfigurationsDB.getAllTwitter().map { conf =>
    val twitterCommunicator = new TwitterCommunicator(conf.twitterApi, Http())
    val questionsActor = actorSystem.actorOf(QuestionsActor.props(questionsURL, authKey, Some(twitterCommunicator)))
    val latestTS = Math.max(TimeCacheTwitter.getLatestTime(conf.tag), conf.lastTimestamp)
    TimeCacheTwitter.updateTimeAndTag(conf.tag, latestTS)
    actorSystem.scheduler.schedule(500 millis, 1 minute) {
      questionsActor ! Fetch(conf.tag, TimeCacheTwitter.getLatestTime(conf.tag))
    }
    actorSystem.scheduler.schedule(500 millis, 10 minute) {
      questionsActor ! CallHeroku(conf.twitterApi.herokuURL)
    }
  }
  SaveConfigurationsDB.getAllSlack().map {
    case (slack, tag, lastTimestamp) =>
      val slackCommunicator = new SlackCommunicator(slack)
      val questionsActor = actorSystem.actorOf(QuestionsActor.props(questionsURL, authKey, None, Some(slackCommunicator)))
      val latestTS = Math.max(TimeCacheSlack.getLatestTime(tag), lastTimestamp)
      TimeCacheSlack.updateTimeAndTag(tag, latestTS)
      actorSystem.scheduler.schedule(500 millis, 1 minute) {
        questionsActor ! Fetch(tag, TimeCacheSlack.getLatestTime(tag))
      }
  }
}

object TimeCacheTwitter {
  private val tagAndTime = mutable.Map[String, Double]()

  def getLatestTime(tag: String): Double = tagAndTime.getOrElse(tag, new DateTime(DateTimeZone.UTC).withTimeAtStartOfDay().getMillis / 1000)

  def updateTimeAndTag(tag: String, time: Double) = tagAndTime.update(tag, time)
}

object TimeCacheSlack {
  private val tagAndTime = mutable.Map[String, Double]()

  def getLatestTime(tag: String): Double = tagAndTime.getOrElse(tag, new DateTime(DateTimeZone.UTC).withTimeAtStartOfDay().getMillis / 1000)

  def updateTimeAndTag(tag: String, time: Double) = tagAndTime.update(tag, time)
}