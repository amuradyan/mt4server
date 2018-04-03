package confs

/**
  * Created by spectrum on 3/29/2018.
  */
object BrokerConfs extends Enumeration {

  case class BrokerConfig(host: String, port: Int, tickers: Seq[String]) extends super.Val

  val demo = BrokerConfig("localhost", 6000, Seq("BTCUSD", "ETHUSD", "XRPUSD"))

  val fxclub = BrokerConfig("localhost", 6001, Seq("BTCUSD", "BTCEUR", "ETCUSD", "LTCBTC", "LTCUSD", "NEOUSD", "ETHUSD",
    "DSHUSD", "XRPUSD", "BCHUSD", "BTGUSD", "XMRUSD", "ZECUSD"))

  val ava = BrokerConfig("localhost", 6002, Seq("BCHUSD", "BTCEUR", "BTCJPY", "BTCUSD", "BTGUSD"))

  val fxopen = BrokerConfig("localhost", 6003, Seq("EURUSD", "USDJPY", "DSHBTC", "DSHUSD", "BCHUSD", "BTCEUR", "BTCUSD",
    "BTCJPY", "LTCBTC", "LTCEUR", "LTCJPY", "LTCUSD", "ETHEUR", "ETHJPY", "ETHUSD", "XRPEUR", "XRPUSD"))

  val icmarkets = BrokerConfig("localhost", 6004, Seq("BCHUSD", "BTCUSD", "DSHUSD", "ETHUSD", "LTCUSD"))

  def getByBrokerName(brokerId: String) = values find {
    _.toString().equalsIgnoreCase(brokerId)
  }
}
