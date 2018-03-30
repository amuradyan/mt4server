package confs

/**
  * Created by spectrum on 3/29/2018.
  */
object BrokerConfs extends Enumeration {

  case class BrokerConfig(host: String, port: Int, tickers: Seq[String]) extends super.Val

  val demo = BrokerConfig("localhost", 6666, Seq("BTCUSD", "ETHUSD", "XRPUSD"))
  val ava = BrokerConfig("localhost", 6666, Seq("BCHUSD","BTCEUR","BTCJPY","BTCUSD","BTGUSD"))
  val ictrade = BrokerConfig("localhost", 6666, Seq("BCHUSD","BTCUSD","DSHUSD","ETHUSD","LTCUSD"))
  val fxopen = BrokerConfig("localhost", 6666, Seq())
  val fxclub = BrokerConfig("localhost", 6666, Seq("BTCUSD","BTCEUR","ETCUSD","LTCBTC","LTCUSD","NEOUSD","ETHUSD","DSHUSD","XRPUSD","BCHUSD","BTGUSD","XMRUSD","ZECUSD"))

  def getByBrokerName(brokerId: String) = values find { _.toString().equalsIgnoreCase(brokerId) }
}
