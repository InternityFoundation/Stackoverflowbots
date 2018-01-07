package in.internity.twitter

import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken, Tweet}
import in.internity.models.{Question, TwitterApi}

import scala.concurrent.Future


/**
  * @author Shivansh <shiv4nsh@gmail.com>
  * @since 6/1/18
  */
class TwitterCommunicator(twitterApi: TwitterApi) {

  val consumerToken = ConsumerToken(key = twitterApi.consumerKey, secret = twitterApi.consumerSecret)
  val accessToken = AccessToken(key = twitterApi.accessKey, secret = twitterApi.accessSecret)
  private val restClient = TwitterRestClient(consumerToken, accessToken)

  def sendTweet(tweet: String): Future[Tweet] = {
    restClient.createTweet(tweet)
  }

  def formulateTweet(item: Question): String = {
    s"""Checkout this question on StackOverflow:${item.title} : ${item.link} #${item.tags.map(_.replace("-", "_")).mkString(" #")} #internity"""
  }
}
