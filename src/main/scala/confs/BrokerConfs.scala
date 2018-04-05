package confs

/**
  * Created by spectrum on 3/29/2018.
  */
final object BrokerConfs extends Enumeration {

  final case class BrokerConfig(host: String, port: Int, tickers: Seq[String]) extends super.Val

  final val demo = BrokerConfig("localhost", 6000, Seq("BTCUSD", "ETHUSD", "XRPUSD"))

  final val fxclub = BrokerConfig("localhost", 6001, Seq("BTCUSD", "BTCEUR", "ETCUSD", "LTCBTC", "LTCUSD", "NEOUSD", "ETHUSD",
    "DSHUSD", "XRPUSD", "BCHUSD", "BTGUSD", "XMRUSD", "ZECUSD"))

  final val ava = BrokerConfig("localhost", 6002, Seq("BCHUSD", "BTCEUR", "BTCJPY", "BTCUSD", "BTGUSD"))

  final val fxopen = BrokerConfig("localhost", 6003, Seq("EURUSD", "USDJPY", "DSHBTC", "DSHUSD", "BCHUSD", "BTCEUR", "BTCUSD",
    "BTCJPY", "LTCBTC", "LTCEUR", "LTCJPY", "LTCUSD", "ETHEUR", "ETHJPY", "ETHUSD", "XRPEUR", "XRPUSD"))

  final val icmarkets = BrokerConfig("localhost", 6004, Seq("BCHUSD", "BTCUSD", "DSHUSD", "ETHUSD", "LTCUSD"))

  def getByBrokerName(brokerId: String) = values find {
    _.toString().equalsIgnoreCase(brokerId)
  }
}
