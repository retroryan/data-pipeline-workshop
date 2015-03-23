package stockActors

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Props, Actor}
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Session, Cluster}
import com.datastax.driver.core.policies.Policies
import stockActors.DataWriterActor.WriteStock


class DataWriterActor extends Actor {

  val counter = new AtomicLong

  def setupDbConnection():Session  = {
    val cluster = Option(Cluster
      .builder()
      .addContactPoints("127.0.0.1")
      .withRetryPolicy(Policies.defaultRetryPolicy())
      .build())

    cluster.get.connect("stocks")
  }


  def available(addStockDatePreparedStatement:PreparedStatement, session:Session) : Receive = {
    case WriteStock(stockData) => writeStockDb(stockData, addStockDatePreparedStatement, session)
  }

  def receive = {
    val session = setupDbConnection()
    val addStockDatePreparedStatement = session.prepare(DataWriterActor.ADD_STOCK_DATE)

    //stock price is turned off by default
    //val addStockPricePreparedStatement = session.prepare(DataWriterActor.ADD_STOCK_PRICE)

    available(addStockDatePreparedStatement, session)
  }

  def writeStockDb(stockData: StockData, addStockDatePreparedStatement:PreparedStatement, session:Session) = {
    writeStockDateDb(stockData, addStockDatePreparedStatement, session)

    //this duplicates every entry to be sorted by price, akka materialized view
    //turned off by default because it doubles the size of the database
    //writeStockPriceDb(stockData)

    //this can be used to verify the right number of records are being added to the db
    counter.getAndIncrement
    if (counter.get % 500 == 0)
      println(s"Total: ${counter.get()}")

    //println(s"Total: ${counter.get()} Added stock to history table: ${stockData.symbol} ${stockData.price} ${stockData.dateOffset}")
  }

  def writeStockDateDb(stockData: StockData, addStockDatePreparedStatement:PreparedStatement, session:Session) = {
    val bind: BoundStatement = addStockDatePreparedStatement.bind()
    bind.setUUID("tradeId", stockData.tradeId)
    bind.setString("symbol", stockData.symbol)
    bind.setInt("dateOffset", stockData.dateOffset)
    bind.setDate("tradeDate", stockData.tradeDate.toDate)
    bind.setDecimal("price", stockData.price.underlying())
    bind.setInt("quantity", stockData.quantity)
    session.execute(bind)
  }

  def writeStockPriceDb(stockData: StockData, addStockPricePreparedStatement:PreparedStatement, session:Session) = {
    val bind: BoundStatement = addStockPricePreparedStatement.bind()
    bind.setUUID("tradeId", stockData.tradeId)
    bind.setString("symbol", stockData.symbol)
    bind.setInt("dateOffset", stockData.dateOffset)
    bind.setDate("tradeDate", stockData.tradeDate.toDate)
    bind.setInt("priceAvg", stockData.price.rounded.intValue())
    bind.setDecimal("price", stockData.price.underlying())
    bind.setInt("quantity", stockData.quantity)
    session.execute(bind)
  }
}


object DataWriterActor {

  val ADD_STOCK_PRICE = "INSERT INTO stock_ticks_price (tradeId, symbol, dateOffset, tradeDate, priceAvg, price, quantity) VALUES (?, ?,?,?,?,?,?)"

  val ADD_STOCK_DATE = "INSERT INTO stock_ticks_date (tradeId, symbol, dateOffset, tradeDate, price, quantity) VALUES (?,?,?,?,?,?)"

  def props(): Props =
    Props(new DataWriterActor())

  case class WriteStock(stockData: StockData)
}