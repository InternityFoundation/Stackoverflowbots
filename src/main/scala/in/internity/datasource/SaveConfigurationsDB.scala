package in.internity.datasource

import in.internity.models.{Configuration, Slack, TwitterApi}

import scala.util.{Failure, Success, Try}

/**
  * @author Shivansh <shiv4nsh@gmail.com>
  * @since 7/1/18
  */
object SaveConfigurationsDB {

  val connection = Datasource.connectionPool.getConnection

  def init() = {
    val stmt = connection.createStatement()
    stmt.executeUpdate(
      "CREATE TABLE IF NOT EXISTS twitterNew ( consumerkey TEXT, consumersecret TEXT, accesskey TEXT, accesssecret TEXT, handle TEXT Primary Key, herokuURL TEXT , tag TEXT, latestTimeStamp DECIMAL(10,0) );"
    )

    stmt.executeUpdate(
      "CREATE TABLE IF NOT EXISTS Slack ( channelName TEXT, apitoken TEXT,tag TEXT, latestTimeStamp DECIMAL(10,0) );"
    )
    stmt.close()
  }

  def saveTwitter(twitterApi: TwitterApi, tag: String, latestTimeStamp: Long, herokuURL: String): Unit = {
    Try({
      val stmt = connection.createStatement()
      val query =s"""INSERT INTO twitterNew VALUES ('${twitterApi.consumerKey}','${twitterApi.consumerSecret}','${twitterApi.accessKey}','${twitterApi.accessSecret}','${twitterApi.handler}','$herokuURL',$latestTimeStamp,'$tag');"""
      println(query)
      val result = stmt.executeUpdate(query)
      stmt.close()
      result
    }) match {
      case Success(a) => println(s"Saved and resulted in code:$a")
      case Failure(err) => println(err.getMessage)
    }
  }

  def getAllTwitter(): List[Configuration] = {
    val stmt = connection.createStatement()
    val rs = stmt.executeQuery("SELECT * FROM twitterNew;")
    val stream = new Iterator[Configuration] {
      def hasNext = rs.next()

      def next() = {
        val twitterApi = TwitterApi(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6))
        Configuration(twitterApi, rs.getString(8), rs.getDouble(7).toLong)
      }
    }.toStream
    val list = stream.toList
    println(s"list:::$list")
    rs.close()
    stmt.close()
    list
  }

  def saveSlack(slack: Slack,tag:String,latestTimeStamp:Double)={
    Try({
      val stmt = connection.createStatement()
      val query =s"""INSERT INTO Slack VALUES ('${slack.channelName}','${slack.apiToken}',$latestTimeStamp,'$tag');"""
      println(query)
      val result = stmt.executeUpdate(query)
      stmt.close()
      result
    }) match {
      case Success(a) => println(s"Saved and resulted in code:$a")
      case Failure(err) => println(err.getMessage)
    }
  }

  def getAllSlack(): List[(Slack,String,Double)] = {
    val stmt = connection.createStatement()
    val rs = stmt.executeQuery("SELECT * FROM Slack;")
    val stream = new Iterator[(Slack,String,Double)] {
      def hasNext = rs.next()

      def next() = {
        (Slack(rs.getString(1),rs.getString(2)),
          rs.getString(3),rs.getDouble(4))
      }
    }.toStream
    val list = stream.toList
    println(s"list:::$list")
    rs.close()
    stmt.close()
    list
  }

}
