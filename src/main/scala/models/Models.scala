package models

/**
  * Created by spectrum on 3/29/2018.
  */
final case class Order(id: Int,
                       date: Long,
                       orderType: String,
                       size: Double,
                       symbol: String,
                       price: Double,
                       sl: Double,
                       tp: Double,
                       currentPrice: Double,
                       commission: Double,
                       taxes: Double,
                       swap: Double,
                       profit: Double)

final case class TickerData(name: String,
                            time: Long,
                            bid: Double,
                            ask: Double,
                            last: Double,
                            volume: Double) {
}

final object TickerData {
  def empty = TickerData("", 0, 0, 0, 0, 0)
}