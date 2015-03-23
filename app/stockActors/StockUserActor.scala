package stockActors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import play.Play
import play.api.libs.json.{JsNumber, JsObject, JsString, _}
import stockActors.StockManagerActor.StockHistory

import scala.collection.JavaConverters._

/** The out actor is wired in by Play Framework when this Actor is created.
  * When a message is sent to out the Play Framework then sends it to the client WebSocket.
  *
  * */
class StockUserActor(out: ActorRef) extends Actor with ActorLogging with SettingsActor {

    val stockManagerActor = settings.stockManagerActor

    // watch the default stocks
    val defaultStocks = Play.application.configuration.getStringList("default.stocks")
    for (stockSymbol <- defaultStocks.asScala) {
        stockManagerActor ! StockManagerActor.WatchStock(stockSymbol)
    }

    def receive = {
        //Handle the FetchTweets message to periodically fetch tweets if there is a query available.
        case StockManagerActor.StockUpdate(symbol, price) =>
            val stockUpdateJson: JsObject = JsObject(Seq(
                "type" -> JsString("stockupdate"),
                "symbol" -> JsString(symbol),
                "price" -> JsNumber(price.doubleValue())
            ))
            out ! stockUpdateJson

        case StockHistory(symbol, history) =>
            val jsonHistList = JsArray(history.map(price => JsNumber(price)))
            val stockHistoryJson: JsObject = JsObject(Seq(
                "type" -> JsString("stockhistory"),
                "symbol" -> JsString(symbol),
                "history" -> jsonHistList
            ))
            out ! stockHistoryJson

        case message: JsValue =>
            (message \ "symbol").asOpt[String] match {
                case Some(symbol) =>
                    stockManagerActor ! StockManagerActor.WatchStock(symbol)
                case None => log.error("symbol was not found in json: $message")
            }
    }

    override def postStop() {
        stockManagerActor ! StockManagerActor.UnwatchStock(None)
    }
}

object StockUserActor {
    def props(out: ActorRef) = Props(new StockUserActor(out))
}
