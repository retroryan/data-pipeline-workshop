package stockActors

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import org.joda.time.{DateTime, Days, MutableDateTime}
import stockActors.DataWriterActor.WriteStock
import stockActors.StockActor.FetchLatest
import stockActors.StockManagerActor.{UnwatchStock, StockHistory, WatchStock, StockUpdate}
import stockActors.StockSummaryActor.ReadStock

import scala.collection.immutable.{HashSet, Queue}
import scala.concurrent.duration._
import scala.util.Random

/**
 * There is one StockActor per stock symbol.  The StockActor maintains a list of users watching the stock and the stock
 * values.  Each StockActor updates a rolling dataset of randomly generated stock values.
 */

class StockActor(symbol: String) extends Actor with ActorLogging with SettingsActor {

  val dataWriterActor = settings.dataWriterActor
  val stockSummaryActor = settings.stockSummaryActor

  protected[this] var watchers: HashSet[ActorRef] = HashSet.empty[ActorRef]

  val rand = Random

  // A random data set which uses stockQuote.newPrice to get each data point
  var stockHistory: Queue[Double] = {
    lazy val initialPrices: Stream[Double] = (rand.nextDouble * 800) #:: initialPrices.map(previous => FakeStockQuote.newPrice(previous))
    initialPrices.take(50).to[Queue]
  }

  val epoch = {
    val t = new MutableDateTime()
    t.setDate(0)
    t
  }

  //start at the current date and time and then randomly add minutes at each tick to increase the time more rapidly
  val currentDateTime = new MutableDateTime
  var lastDayOffset = (Days.daysBetween(epoch, currentDateTime)).getDays

  implicit val executionContext = context.system.dispatcher
  // Fetch the latest stock value every 75ms
  val stockTick = context.system.scheduler.schedule(Duration.Zero, 500.millis, self, FetchLatest)

  def receive = {
    case FetchLatest => {
      // add a new stock price to the history and drop the oldest
      val newPrice = FakeStockQuote.newPrice(stockHistory.last.doubleValue())
      stockHistory = stockHistory.drop(1) :+ newPrice
      // notify watchers
      watchers.foreach(_ ! StockUpdate(symbol, newPrice))
    }

    case WatchStock(_) => {
      // send the stock history to the user
      sender ! StockHistory(symbol, stockHistory.toList)
      // add the watcher to the list
      watchers = watchers + sender
    }

    case UnwatchStock(_) => {
      watchers = watchers - sender
      if (watchers.size == 0) {
        stockTick.cancel()
        context.stop(self)
      }
    }
  }
}

object StockActor {

  def props(symbol: String): Props =
    Props(new StockActor(symbol))

  case object FetchLatest
}