package confs

/**
  * Created by spectrum on 3/29/2018.
  */
object BrokerConfs extends Enumeration {

  case class BrokerConfig(host: String, port: Int) extends super.Val

  val demo = BrokerConfig("localhost", 6666)
  val ava = BrokerConfig("localhost", 6666)
  val ictrade = BrokerConfig("localhost", 6666)
  val fxopen = BrokerConfig("localhost", 6666)
  val fxclub = BrokerConfig("localhost", 6666)

  def getByBrokerName(brokerId: String) = values find { _.toString().equalsIgnoreCase(brokerId) }
}
