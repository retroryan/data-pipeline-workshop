package stockActors

import java.util.UUID

import org.joda.time.DateTime

/**
 *
 * @param tradeId
 * @param symbol
 * @param dateOffset - int of days since epoch
 * @param tradeDate - timestampe of trade
 * @param price
 * @param quantity
 */
case class StockData(tradeId: UUID, symbol: String, dateOffset: Int, tradeDate:DateTime, price: BigDecimal, quantity:Int)

object StockData {
  def apply(symbol: String, dateOffset: Int, tradeDate:DateTime, price: BigDecimal, quantity:Int) =
    new StockData(UUID.randomUUID(), symbol, dateOffset, tradeDate, price, quantity)
}
