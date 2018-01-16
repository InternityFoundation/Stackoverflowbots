package in.internity.slack

import com.flyberrycapital.slack.SlackClient
import in.internity.models.{Question, Slack}

/**
  * @author Shivansh <shiv4nsh@gmail.com>
  * @since 15/1/18
  */
class SlackCommunicator(slack:Slack) {

  val s = new SlackClient(slack.apiToken)

  def formulateMessage(question:Question)={
    s"""${question.title} : ${question.link}"""
  }
  def sendMessage( message: String) = {
    s.chat.postMessage(s"#${slack.channelName}", message)
  }

}
