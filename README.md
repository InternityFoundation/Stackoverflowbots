# Stackoverflowbots

It is an app deployed on Heroku that pulls the question from stackoverflow for a particular tag on regular basis and posts it on the Twitter.

You can create your own bot using this app. 

Just send a POST request:

Request : https://internity-bots.herokuapp.com/createBot/<TagName>

Body : 

`{

 "consumerKey": "*********************",
 
 "consumerSecret": "*********************************************",
 
 "accessKey": "************************************************",
 
 "accessSecret": "***************************************************",
 
 "handler":"TwitterHandler"
 
 }`

It will create a bot that will post the tweets on this Twitterhandler.
Some Examples are:
1. [ScalaAtStackoverflow.](https://twitter.com/ScalaAtStack)
2. [JSAtStackOverflow.](https://twitter.com/JSAtStack)


For getting consumerKey, consumerSecret ,accessKey, accessSecret from Twitter log on to [TwitterApps](https://apps.twitter.com)
