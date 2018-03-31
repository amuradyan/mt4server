package mt4commands

import java.io.PrintWriter
import java.net.Socket
import java.util.Scanner

import confs.BrokerConfs
import confs.BrokerConfs.BrokerConfig
import models.{Order, TickerData}
import resp.RESP
import specs.OrderSpec

/**
  * Created by spectrum on 3/29/2018.
  */
case class MT4Commands(val brokerId: String) {
  var out: PrintWriter = null
  var in: Scanner = null
  var socket: Socket = null
  var currentBroker: BrokerConfig = null

  setupConnection

  private def setupConnection = {

    BrokerConfs.getByBrokerName(brokerId) match {
      case Some(brokerConfig: BrokerConfig) => {
        currentBroker = brokerConfig
      }
      case None =>
        throw new IllegalArgumentException("Unknown broker")
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

      println(s"Expecting ${nLines}")

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
      println(lineAtHand)
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

    println(s"Request fullfilled in ${end - start}")
    deleted
  }

  def placeOrder(order: OrderSpec) = {
    var placedOrderId = -1

    out.write(RESP.from(s"buy ${order.pair} ${order.amount}"))
    out.flush()
    if (in.hasNextLine) {
      val lineAtHand = in.nextLine();
      if (!lineAtHand.startsWith("-")) placedOrderId = lineAtHand.substring(1, lineAtHand.length).toInt
    }

    teardown

    placedOrderId
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
          println(lineAtHand)
        })
      }
    }

    teardown

    tickerData
  }

  def getTickerData: Seq[TickerData] =  getTickerData(currentBroker.tickers)
}
