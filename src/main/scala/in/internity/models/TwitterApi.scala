package in.internity.models

/**
  * @author Shivansh <shiv4nsh@gmail.com>
  * @since 7/1/18
  */
case class TwitterApi(
                       consumerKey: String,
                       consumerSecret: String,
                       accessKey: String,
                       accessSecret: String,
                       handler:String
                     )

case class Configuration(twitterApi: TwitterApi, tag: String, lastTimestamp: Long)