package in.internity.twitter

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import in.internity.models.{Owner, Question, TwitterApi}
import org.json4s.native.Serialization
import org.json4s.{DefaultFormats, native}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


/**
  * @author Shivansh <shiv4nsh@gmail.com>
  * @since 6/1/18
  */
class TwitterCommunicator(twitterApi: TwitterApi, http: HttpExt)(implicit as: ActorSystem, mat: Materializer, ec: ExecutionContext) {

  import Json4sSupport._

  implicit val formats: DefaultFormats.type = DefaultFormats
  implicit val serialization: Serialization.type = native.Serialization

  val consumerToken = ConsumerToken(key = twitterApi.consumerKey, secret = twitterApi.consumerSecret)
  val accessToken = AccessToken(key = twitterApi.accessKey, secret = twitterApi.accessSecret)
  private val restClient = TwitterRestClient(consumerToken, accessToken)

  def sendTweet(tweet: String): Unit = {
    val tweet = restClient.createTweet(tweet)
    Await.result(tweet, 1000 millis)
  }

  def formulateTweet(item: Question): Future[String] = {
    getTwitterUsername(item.owner).map { username =>
      val tweet = s"""Checkout this question on StackOverflow:${item.title} : ${item.link} #${item.tags.map(_.replace("-", "_")).mkString(" #")} #internity"""
      if (username.isEmpty) {
        tweet
      } else {
        s"""$tweet by @$username"""
      }
    }
  }

  def getTwitterUsername(owner: Owner): Future[String] = {
    val response = http.singleRequest(HttpRequest(uri = owner.link))
    response.flatMap(responseToString).map { str =>
      str.split("https://twitter.com/*")(1).split("\"")(0).replace("@", "")
    }.recover {
      case _ => ""
    }
  }

  private def responseToString(response: HttpResponse): Future[String] = {
    response.status match {
      case StatusCodes.OK => Unmarshal(response.entity.withContentType(ContentTypes.`text/html(UTF-8)`)).to[String]
      case a =>
        println(s"Failed to get Actual response: Status Code :$a")
        Future.failed(new Exception(s"Failed to get Actual response: Status Code :$a"))
    }
  }
}
