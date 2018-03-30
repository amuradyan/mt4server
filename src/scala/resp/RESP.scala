package resp

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}

import models.{Order, TickerData}
import specs.OrderSpec

/**
  * Created by spectrum on 3/29/2018.
  */
object RESP {
  val dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")

  def from(input: AnyRef): String = {
    val sb = new StringBuilder()

    input match {
      case s: String => {
        val commandBlocks = s.split(" ")
        sb.append(s"*${commandBlocks.length}\r\n")
        commandBlocks foreach {
          c => sb.append(s"$$${c.length}\r\n$c\r\n")
        }
      }
      case os: OrderSpec => {
        sb.append("*3\r\n")

        if (os.amount > 0)
          sb.append("$3\r\nbuy\r\n")
        else if (os.amount < 0)
          sb.append("$4\r\nsell\r\n")
        else
          throw new IllegalArgumentException("Amount of order can't be 0")

        sb.append(s"$$${os.pair.length}\r\n${os.pair}\r\n")
        sb.append(s"$$${os.amount.toString.length}\r\n${os.amount}")
      }
      case _ => throw new IllegalArgumentException("Unknown input")
    }

    println(s"From: $input to ${sb.toString()}")

    sb.toString
  }

  def toBalance(respBalance: String) = respBalance.substring(1, respBalance.length).toDouble

  def toOrder(respOrder: String) = {
    val orderMembers = respOrder.split(" ")

    val id = orderMembers(0).substring(2, orderMembers(0).length).toInt
    val date = LocalDateTime.parse(s"${orderMembers(1)} ${orderMembers(2)}", dtf).toEpochSecond(ZoneOffset.UTC)
    val orderType = orderMembers(3)
    val size = orderMembers(4).toDouble
    val symbol = orderMembers(5)
    val price = orderMembers(6).toDouble
    val sl = orderMembers(7).toDouble
    val tp = orderMembers(8).toDouble
    val currentPrice = orderMembers(9).toDouble
    val commission = orderMembers(10).toDouble
    val taxes = orderMembers(11).toDouble
    val swap = orderMembers(12).toDouble
    val profit = orderMembers(13).toDouble

    Order(id, date, orderType, size, symbol, price, sl, tp, currentPrice, commission, taxes, swap, profit)
  }

  //  #BTCUSD 2018.03.30 11:16:31 7063.73 7143.73 0.00 0.000000
  def toTickerData(respTickerdata: String) = {
    val tickerDataMembers = respTickerdata.split(" ")

    val tickerName = tickerDataMembers(0).substring(2, tickerDataMembers(0).length)
    val date = LocalDateTime.parse(s"${tickerDataMembers(1)} ${tickerDataMembers(2)}", dtf).toEpochSecond(ZoneOffset.UTC)
    val bidPrice = tickerDataMembers(3).toDouble
    val askPrice = tickerDataMembers(4).toDouble
    val lastPrice = tickerDataMembers(5).toDouble
    val volume = tickerDataMembers(6).toDouble

    TickerData(tickerName, date, bidPrice, askPrice, lastPrice, volume)
  }
}