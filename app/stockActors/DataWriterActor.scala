package stockActors

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Props, Actor}
import com.datastax.driver.core.{BoundStatement, PreparedStatement, Session, Cluster}
import com.datastax.driver.core.policies.Policies
import stockActors.DataWriterActor.WriteStock

/**
 * This Actor Class Writes Data to Cassandra.
 * It hard codes the connection to a local cassandra instance.  A production setup would make several changes like:
 *
 * 1 - Setup a global connection to cassandra.  This would best be done in an Actor Extension like Settings
 *
 * 2 - Read the db config from a config file, like the play config file.
 */
class DataWriterActor extends Actor {

  val counter = new AtomicLong

  def setupDbConnection(): Session = {
    val cluster = Cluster
      .builder()
      .addContactPoints("127.0.0.1")
      .withRetryPolicy(Policies.defaultRetryPolicy())
      .build()

    //This creates a new session on this cluster, initialize it and sets it to the
    //keyspace provided
    cluster.connect("stocks")
  }

  /**
   * This is the initial actor behavior which sets up the cassandra session and creates a prepared statement
   * that is used to to write to cassandra.
   * @return
   */
  def receive = {
    val session = setupDbConnection()
    val addStockDatePreparedStatement = session.prepare(DataWriterActor.ADD_STOCK_DATE)

    //by returning available we are make the default receive behavior be available.
    available(addStockDatePreparedStatement, session)
  }

  /**
   * This defines is the actor receive block that will be used to handle write messages.
   * It is defined in a message so that the actor processing can transition to this state once it is setup.
   */
  def available(addStockDatePreparedStatement: PreparedStatement, session: Session): Receive = {
    case WriteStock(stockData) => writeStockDate(stockData, addStockDatePreparedStatement, session)
  }

  /**
   * Bind the stock data to the prepared statement and execute the insert on the session.
   *
   * This actually stores the data in the database.
   *
   * @param stockData
   * @param addStockDatePreparedStatement
   * @param session
   * @return
   */
  def writeStockDate(stockData: StockData, addStockDatePreparedStatement: PreparedStatement, session: Session) = {
    val bind: BoundStatement = addStockDatePreparedStatement.bind()
    bind.setUUID("tradeId", stockData.tradeId)
    bind.setString("symbol", stockData.symbol)
    bind.setInt("dateOffset", stockData.dateOffset)
    bind.setDate("tradeDate", stockData.tradeDate.toDate)
    bind.setDecimal("price", stockData.price.underlying())
    bind.setInt("quantity", stockData.quantity)
    session.execute(bind)

    //this can be used to verify the right number of records are being added to the db
    counter.getAndIncrement
    if (counter.get % 500 == 0)
      println(s"Total: ${counter.get()}")

  }
}

object DataWriterActor {

  val ADD_STOCK_DATE = "INSERT INTO stock_ticks_date (tradeId, symbol, dateOffset, tradeDate, price, quantity) VALUES (?,?,?,?,?,?)"

  def props(): Props =
    Props(new DataWriterActor())

  case class WriteStock(stockData: StockData)

}