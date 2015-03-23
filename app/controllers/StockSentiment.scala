package controllers

import play.api.libs.json.{JsString, JsValue, Json}
import play.api.libs.ws._
import play.api.mvc._
import play.libs.Akka
import stockActors.Settings

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.Play.current

object StockSentiment extends Controller {

  case class Tweet(text: String)

  implicit val tweetReads = Json.reads[Tweet]

  def getTextSentiment(text: String): Future[WSResponse] =
    WS.url(Settings(Akka.system).sentimentUrl) post Map("text" -> Seq(text))

  def getAverageSentiment(responses: Seq[WSResponse], label: String): Double = responses.map { response =>
    (response.json \\ label).head.as[Double]
  }.sum / responses.length.max(1) // avoid division by zero

  def loadSentimentFromTweets(json: JsValue): Seq[Future[WSResponse]] =
    (json \ "statuses").as[Seq[Tweet]] map (tweet => getTextSentiment(tweet.text))

  def getTweets(symbol: String): Future[WSResponse] = {
    val symbolTweetUrl = Settings(Akka.system).defaultTweetUrl.format(symbol)
    WS.url(symbolTweetUrl).get.withFilter { response =>
      response.status == OK
    }
  }


  def sentimentJson(sentiments: Seq[WSResponse]) = {
    val neg = getAverageSentiment(sentiments, "neg")
    val neutral = getAverageSentiment(sentiments, "neutral")
    val pos = getAverageSentiment(sentiments, "pos")

    val response = Json.obj(
      "probability" -> Json.obj(
        "neg" -> neg,
        "neutral" -> neutral,
        "pos" -> pos
      )
    )

    val classification =
      if (neutral > 0.5)
        "neutral"
      else if (neg > pos)
        "neg"
      else
        "pos"

    response + ("label" -> JsString(classification))
  }

  def get(symbol: String): Action[AnyContent] = Action.async {
    val futureStockSentiments: Future[Result] = for {
      tweets <- getTweets(symbol) // get tweets that contain the stock symbol
      futureSentiments = loadSentimentFromTweets(tweets.json) // queue web requests each tweets' sentiments
      sentiments <- Future.sequence(futureSentiments) // when the sentiment responses arrive, set them
    } yield Ok(sentimentJson(sentiments))

    futureStockSentiments.recover {
      case nsee: NoSuchElementException =>
        InternalServerError(Json.obj("error" -> JsString("Could not fetch the tweets")))
    }
  }
}
