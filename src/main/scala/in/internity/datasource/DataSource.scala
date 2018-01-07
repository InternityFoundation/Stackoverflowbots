package in.internity.datasource

import java.net.URI

import org.apache.commons.dbcp2._

/**
  * @author Shivansh <shiv4nsh@gmail.com>
  * @since 7/1/18
  */
object Datasource {
  val systemDbURL = System.getenv("DATABASE_URL")
  println(systemDbURL)
  val dbUri = new URI(systemDbURL)
  println(dbUri)
  val dbUrl = s"jdbc:postgresql://${dbUri.getHost}:${dbUri.getPort}${dbUri.getPath}"
  val connectionPool = new BasicDataSource()

  if (dbUri.getUserInfo != null) {
    connectionPool.setUsername(dbUri.getUserInfo.split(":")(0))
    connectionPool.setPassword(dbUri.getUserInfo.split(":")(1))
  }
  connectionPool.setDriverClassName("org.postgresql.Driver")
  connectionPool.setUrl(dbUrl)
  connectionPool.setInitialSize(3)
}
