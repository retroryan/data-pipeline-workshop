package stockActors

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Props, Actor}
import com.datastax.driver.core.policies.Policies
import com.datastax.driver.core._
import org.joda.time.{DateTime, MutableDateTime}
import stockActors.StockSummaryActor.ReadStock

import scala.concurrent.Future


class StockSummaryActor extends Actor {

  val counter = new AtomicLong

  var epoch = {
    var t = new MutableDateTime()
    t.setDate(0)
    t
  }

  def setupDbConnection() = {
    val cluster = Cluster
      .builder()
      .addContactPoints("127.0.0.1")
      .withRetryPolicy(Policies.defaultRetryPolicy())
      .build()

    cluster.connect("stocks")
  }

  implicit val executionContext = context.system.dispatcher

  def available(selectStockTicks:PreparedStatement, insertStockSummary:PreparedStatement, session:Session) : Receive = {
    case ReadStock(symbol, dateOffset) => {
      Future {
        fetchStocksBySymbolDate(symbol, dateOffset, selectStockTicks, insertStockSummary, session)
      }
    }
  }

  def receive = {
    val session = setupDbConnection()
    val selectStockTicks = session.prepare(StockSummaryActor.SELECT_STOCK_DATE)
    val insertStockSummary = session.prepare(StockSummaryActor.INSERT_STOCK_SUMMARY)

    available(selectStockTicks, insertStockSummary, session)
  }

  def fetchStocksBySymbolDate(symbol: String, dateOffset: Int, selectStockTicks:PreparedStatement, insertStockSummary:PreparedStatement, session:Session) = {
    println(s"summarizing stock $symbol for epoch offset $dateOffset")
    val boundSelectStock: BoundStatement = selectStockTicks.bind()
    boundSelectStock.setString(0, symbol)
    boundSelectStock.setInt(1, dateOffset)

    val resultSet = session.execute(boundSelectStock)
    var stockDataSet: Vector[StockData] = Vector.empty[StockData]

    while (resultSet.iterator().hasNext) {
      val nextRow: Row = resultSet.one()
      val tradeId = nextRow.getUUID("tradeId")
      val symbol = nextRow.getString("symbol")
      val dateOffset = nextRow.getInt("dateOffset")
      val tradeDate = nextRow.getDate("tradeDate")
      val price = nextRow.getDecimal("price")
      val quantity = nextRow.getInt("quantity")

      val sd = StockData(tradeId, symbol, dateOffset, new DateTime(tradeDate), price, quantity)
      stockDataSet = stockDataSet :+ sd
    }

    if (stockDataSet.size > 1) {
      val open = stockDataSet.head
      val close = stockDataSet.last
      val high = stockDataSet.maxBy(sd => sd.price)
      val low = stockDataSet.minBy(sd => sd.price)
      val totQuantity = stockDataSet.foldLeft(0) {
        (sum, sd) => sum + sd.quantity
      }

      val insertSummaryBind: BoundStatement = insertStockSummary.bind()
      insertSummaryBind.setString("symbol", symbol)
      insertSummaryBind.setInt("dateOffset", dateOffset)
      insertSummaryBind.setDecimal("open", open.price.underlying())
      insertSummaryBind.setDecimal("close", close.price.underlying())
      insertSummaryBind.setDecimal("high", high.price.underlying())
      insertSummaryBind.setDecimal("low", low.price.underlying())
      insertSummaryBind.setInt("totQuantity", totQuantity)
      session.execute(insertSummaryBind)
    }
  }
}


object StockSummaryActor {

  val SELECT_STOCK_DATE = "select * from stock_ticks_date where symbol=? and dateOffset=? order by tradeDate ASC"
  val INSERT_STOCK_SUMMARY = "INSERT INTO stock_summary (symbol, dateOffset, open, close, high, low, totQuantity) VALUES (?,?,?,?,?,?,?)"

  def props(): Props =
    Props(new StockSummaryActor())

  case class ReadStock(symbol: String, dateOffset: Int)

}