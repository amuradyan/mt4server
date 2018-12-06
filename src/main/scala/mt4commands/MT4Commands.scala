package mt4commands

import java.io.PrintWriter
import java.net.Socket
import java.util.Scanner

import com.typesafe.scalalogging.Logger
import confs.{BrokerConfig, BrokerConfs}
import models.{Order, TickerData}
import resp.RESP
import specs.OrderSpec

import scala.collection.JavaConverters._

/**
  * Created by spectrum on 3/29/2018.
  */
final case class MT4Commands(private val brokerId: String) {
  private var out: PrintWriter = null
  private var in: Scanner = null
  private var socket: Socket = null
  private var currentBroker: BrokerConfig = null
  val logger = Logger[MT4Commands]

  setupConnection

  private def setupConnection = {

    BrokerConfs.getBrokerByName(brokerId) match {
      case Some(brokerConfig: BrokerConfig) => {
        currentBroker = brokerConfig
      }
      case None =>
        throw new IllegalArgumentException(s"Unknown broker: ${brokerId}")
    }
    socket = new Socket(currentBroker.host, currentBroker.port)
    out = new PrintWriter(socket.getOutputStream())
    in = new Scanner(socket.getInputStream())
  }

  private def teardown = {
    in.close
    out.close
    socket.close
  }

  def getOrders = {
    var orders = Seq[Order]()

    out.write(RESP.from("orders"))
    out.flush
    if (in.hasNextLine) {
      var lineAtHand = in.nextLine()
      println(s"First line: ${lineAtHand}")
      val nLines = lineAtHand.substring(1, lineAtHand.length).toInt

      logger.info(s"Expecting ${nLines}")

      1 to nLines foreach (i => {
        lineAtHand = in.nextLine()
        orders :+= RESP.toOrder(lineAtHand)
        println(lineAtHand)
      })
    }

    teardown

    orders
  }

  def balance = {
    var balance = 0.0

    out.write(RESP.from("balance"))
    out.flush
    if (in.hasNextLine) {
      val lineAtHand = in.nextLine
      balance = RESP.toBalance(lineAtHand)
      logger.info(lineAtHand)
    }
    teardown

    balance;
  }

  def deleteOrder(orderId: Int) = {
    val start = System.currentTimeMillis()

    var deleted = false

    out.write(RESP.from(s"close $orderId"))
    out.flush
    if (in.hasNextLine) {
      val lineAtHand = in.nextLine
      deleted = lineAtHand.equals("+Ok")
    }
    teardown

    val end = System.currentTimeMillis()

    logger.info(s"Request fullfilled in ${end - start}")
    deleted
  }

  def placeOrder(order: OrderSpec) = {
    var placedOrderId = -1

    out.write(RESP.from(s"${order.op} ${order.pair} ${order.amount}"))
    out.flush()
    if (in.hasNextLine) {
      val lineAtHand = in.nextLine();
      if (!lineAtHand.startsWith("-")) placedOrderId = lineAtHand.substring(1, lineAtHand.length).toInt
    }

    teardown

    placedOrderId
  }

  def getSingleTickerData(ticker: String) = {
    var tickerData = TickerData.empty

    out.write(RESP.from("ticker " + ticker))
    out.flush()

    if (in.hasNextLine) {
      val lineAtHand = in.nextLine
      if (lineAtHand.startsWith("+")) {
        tickerData = RESP.toTickerData(lineAtHand)
        logger.info(lineAtHand)
      } else {
        logger.info(s"Invalid response: ${lineAtHand}")
      }
    }

    teardown

    tickerData
  }

  def getTickerData(tickers: Seq[String]) = {
    var tickerData = Seq[TickerData]()

    out.write(RESP.from("tickers " + tickers.mkString(" ")))
    out.flush()

    if (in.hasNextLine) {
      var lineAtHand = in.nextLine
      if (!lineAtHand.startsWith("-")) {
        val nLines = lineAtHand.substring(1, lineAtHand.length).toInt

        1 to nLines foreach (i => {
          lineAtHand = in.nextLine()
          tickerData :+= RESP.toTickerData(lineAtHand)
          logger.info(lineAtHand)
        })
      }
    }

    teardown

    tickerData
  }

  def getTickerData: Seq[TickerData] =  getTickerData(currentBroker.tickers asScala)
}
