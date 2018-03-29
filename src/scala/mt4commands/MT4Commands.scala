package mt4commands

import java.io.PrintWriter
import java.net.Socket
import java.util.Scanner

import confs.BrokerConfs
import confs.BrokerConfs.BrokerConfig
import models.Order
import resp.RESP
import specs.OrderSpec

/**
  * Created by spectrum on 3/29/2018.
  */
case class MT4Commands(val brokerId: String) {
  var out: PrintWriter = null
  var in: Scanner = null
  var socket: Socket = null

  private def setupConnection = {

    BrokerConfs.getByBrokerName(brokerId) match {
      case Some(config: BrokerConfig) => {
        socket = new Socket(config.host, config.port)
        out = new PrintWriter(socket.getOutputStream())
        in = new Scanner(socket.getInputStream())
      }
      case None =>
        throw new IllegalArgumentException("Unknown broker")
    }
  }

  private def teardown = {
    in.close
    out.close
    socket.close
  }

  def getOrders = {
    var orders = Seq[Order]()

    setupConnection

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

    setupConnection
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

    setupConnection
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
    setupConnection

    out.write(RESP.from(s"buy ${order.pair} ${order.amount}"))
    out.flush()
    if (in.hasNextLine) {
      val lineAtHand = in.nextLine();
      if (!lineAtHand.startsWith("-")) placedOrderId = lineAtHand.substring(1, lineAtHand.length).toInt
    }
    teardown

    placedOrderId
  }
}
