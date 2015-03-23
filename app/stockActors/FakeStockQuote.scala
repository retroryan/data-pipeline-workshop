package stockActors

import scala.util.Random


trait StockQuote {
    def newPrice(lastPrice: Double): Double
}

object FakeStockQuote extends StockQuote {

    val rand = Random

    def newPrice(lastPrice: Double): Double = {
        lastPrice * (0.95 + (0.1 * rand.nextDouble))
    }

}
