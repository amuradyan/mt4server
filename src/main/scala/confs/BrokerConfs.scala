package confs


import java.util

import com.typesafe.scalalogging.Logger

import scala.util.control.Breaks._

/**
  * Created by spectrum on 3/29/2018.
  */
final class BrokerConfig(var name: String, var host: String, var port: Int, var tickers: util.ArrayList[String]) {
  def this() = this("", "", -1, new util.ArrayList())
}

final class BrokerConfs
final object BrokerConfs {
  val logger = Logger[BrokerConfs]
  var confs: List[BrokerConfig] = List()
  def initFrom(confsMap: util.ArrayList[util.HashMap[String, AnyRef]]): Unit = {
    confsMap forEach {
      conf => breakable {
        val brokerConfig = new BrokerConfig()
        conf.get("name") match {
          case name: String => brokerConfig.name = name
          case null => {
            logger.error("Unable to find a 'name' key")
            break
          }
        }
        conf.get("host") match {
          case host: String => brokerConfig.host = host
          case null => {
            logger.error("Unable to find a 'host' key")
            break
          }
        }
        conf.get("port") match {
          case port: Integer => brokerConfig.port = port
          case null => {
            logger.error("Unable to find a 'port' key")
            break
          }
        }
        conf.get("tickers") match {
          case tickers: util.ArrayList[String] => brokerConfig.tickers = tickers
          case null => {
            logger.error("Unable to find a 'tickers' key")
            break
          }
        }

        logger.info(s"Adding ${brokerConfig.name} to available brokers")
        confs :+= brokerConfig
      }
    }
  }

  def getBrokerByName(name: String) = confs.find(_.name.equals(name))
}
