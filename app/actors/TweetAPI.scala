package actors

import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.ws._
import play.api.libs.ws.ning.{NingWSClient, NingAsyncHttpClientConfigBuilder}
import play.api.libs.ws.DefaultWSClientConfig

object TweetAPI {

    val builder = new NingAsyncHttpClientConfigBuilder(DefaultWSClientConfig())
    val client: WSClient = new NingWSClient(builder.build())

    val TWEET_PROXY_URL = "http://search-twitter-proxy.herokuapp.com/search/tweets"
    val GEOCODE_URL = "http://maps.googleapis.com/maps/api/geocode/json"

    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    def tweetWS(query: String):Future[WSResponse] =
        client.url(TWEET_PROXY_URL).withQueryString("q" -> query).get()

    def geocodeWS(query: String):Future[WSResponse] =
        client.url(GEOCODE_URL).withQueryString(
            "sensor" -> "false",
            "address" -> query)
            .get()

}

