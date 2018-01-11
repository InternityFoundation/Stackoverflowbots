package in.internity.datasource

import in.internity.models.{Configuration, TwitterApi}

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
    stmt.close()
  }

  def save(twitterApi: TwitterApi, tag: String, latestTimeStamp: Long, herokuURL: String): Unit = {
    Try({
      val stmt = connection.createStatement()
      val query =s"""INSERT INTO twitterNew VALUES ('${twitterApi.consumerKey}','${twitterApi.consumerSecret}','${twitterApi.accessKey}','${twitterApi.accessSecret}','${twitterApi.handler}','$herokuURL','$tag',$latestTimeStamp );"""
      val result = stmt.executeUpdate(query)
      stmt.close()
      result
    }) match {
      case Success(a) => println(s"Saved and resulted in code:$a")
      case Failure(err) => println(err.getMessage)
    }
  }

  def getAll(): List[Configuration] = {
    val stmt = connection.createStatement()
    val rs = stmt.executeQuery("SELECT * FROM twitterNew;")
    val stream = new Iterator[Configuration] {
      def hasNext = rs.next()

      def next() = {
        val twitterApi = TwitterApi(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6))
        Configuration(twitterApi, rs.getString(7), rs.getDouble(8).toLong)
      }
    }.toStream
    val list = stream.toList
    rs.close()
    stmt.close()
    list
  }
}
