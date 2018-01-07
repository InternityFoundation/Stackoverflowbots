package in.internity.twitter

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.Materializer
import in.internity.models.{Owner, Question, TwitterApi}
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, native}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


/**
  * @author Shivansh <shiv4nsh@gmail.com>
  * @since 6/1/18
  */
class TwitterCommunicator(twitterApi: TwitterApi, http: HttpExt)(implicit as: ActorSystem, mat: Materializer, ec: ExecutionContext) {

  implicit val formats: DefaultFormats.type = DefaultFormats
  implicit val serialization: Serialization.type = native.Serialization

  import twitter4j.conf.ConfigurationBuilder
  import twitter4j.{Twitter, TwitterFactory}

  val cb = new ConfigurationBuilder
  cb.setDebugEnabled(true)
    .setOAuthConsumerKey(twitterApi.consumerKey)
    .setOAuthConsumerSecret(twitterApi.consumerSecret)
    .setOAuthAccessToken(twitterApi.accessKey)
    .setOAuthAccessTokenSecret(twitterApi.accessSecret)
  val tf = new TwitterFactory(cb.build)
  val twitter: Twitter = tf.getInstance

  def sendTweet(tweet: String): Unit = {
    val resultantTweet = twitter.updateStatus(tweet)
    println(s"resultantTweet Status:${resultantTweet.getId}")
  }

  def formulateTweet(item: Question): Future[String] = {
    getTwitterUsername(item.owner).map { username =>
      val tweet = s"""Checkout this question on StackOverflow:${item.title} : ${item.link} #${item.tags.map(_.replace("-", "_")).mkString(" #")} #internity"""
      if (username.isEmpty || username.equalsIgnoreCase("stackoverflow")) {
        tweet
      } else {
        s"""$tweet by @$username"""
      }
    }
  }

  def getTwitterUsername(owner: Owner): Future[String] = {
    println(s"OwnerLink:${owner.link}")
    val response = http.singleRequest(HttpRequest(uri = owner.link))
    response.flatMap(responseToString).map { str =>
      val username = Try(str.split("https://twitter.com/*")(1).split("\"")(0).replace("@", "")).toOption.getOrElse("")
      username
    }
  }

  private def responseToString(response: HttpResponse): Future[String] = {
    response.status match {
      case StatusCodes.OK =>
        val str = response.entity.withContentType(ContentTypes.`text/html(UTF-8)`).toStrict(1000 millis).map {
          _.data
        }
        str.map(_.utf8String)
      case a =>
        println(s"Failed to get Actual response: Status Code :$a")
        Future.failed(new Exception(s"Failed to get Actual response: Status Code :$a"))
    }
  }
}
