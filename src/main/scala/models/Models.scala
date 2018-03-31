package models

/**
  * Created by spectrum on 3/29/2018.
  */
case class Order(id: Int,
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

case class TickerData(name: String,
                      time: Long,
                      bid: Double,
                      ask: Double,
                      last: Double,
                      volume: Double)