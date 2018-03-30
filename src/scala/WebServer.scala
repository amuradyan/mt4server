import java.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import akka.stream.ActorMaterializer
import com.google.gson.Gson
import confs.BrokerConfs
import confs.BrokerConfs.BrokerConfig
import models.{Order, TickerData}
import mt4commands.MT4Commands
import specs.OrderSpec

import scala.concurrent.Future
import scala.io.StdIn

trait CsvParameters {
  implicit def csvSeqParamMarshaller: FromStringUnmarshaller[Seq[String]] =
    Unmarshaller(ex ⇒ s ⇒ Future.successful(s.split(",")))

  implicit def csvListParamMarshaller: FromStringUnmarshaller[List[String]] =
    Unmarshaller(ex ⇒ s ⇒ Future.successful(s.split(",").toList))
}

object CsvParameters extends CsvParameters

/**
  * Created by spectrum on 3/27/2018.
  */
object WebServer {
  def main(args: Array[String]) {
    implicit val actorSystem = ActorSystem("musho")
    implicit val materializer = ActorMaterializer()
    implicit val executionCtx = actorSystem.dispatcher

    import CsvParameters._

    val route = {
      pathSingleSlash {
        complete("It's alive!!!")
      } ~
      pathPrefix("exchanges") {
        pathPrefix(Segment) {
          exchangeId => {
            pathPrefix("orders") {
              pathEnd {
                get {
                  val ordersList = new util.ArrayList[Order]()
                  MT4Commands(exchangeId).getOrders foreach ordersList.add

                  complete(new Gson().toJson(ordersList))
                }
              } ~
                post {
                  entity(as[String]) {
                    orderSpecJson => {
                      val orderSpec = new Gson().fromJson(orderSpecJson, classOf[OrderSpec])
                      complete(s"Order created for ${orderSpec.amount} of ${orderSpec.pair} in exchange $exchangeId")
                    }
                  }
                }
            } ~
            pathPrefix("balance") {
              pathEnd {
                get {
                  complete(MT4Commands(exchangeId).balance.toString)
                }
              } ~
                path(IntNumber) {
                  orderId => {
                    pathEnd {
                      delete {
                        complete(MT4Commands(exchangeId).deleteOrder(orderId).toString)
                      }
                    }
                  }
                }
            } ~
            pathPrefix("tickers") {
              path(Segment) {
                ticker => {
                  get {
                    complete(s"Replying with data of ticker $ticker")
                  }
                }
              } ~
              get {
                parameters('name.as[List[String]].?) {
                  tickers => {
                    val tickerDataList = new util.ArrayList[TickerData]()
                    tickers match {
                      case Some(tickers: List[String]) => {
                        MT4Commands(exchangeId).getTickerData(tickers) foreach tickerDataList.add
                      }
                      case None => {
                        MT4Commands(exchangeId).getTickerData foreach tickerDataList.add
                      }
                    }
                    complete(new Gson().toJson(tickerDataList))
                  }
                }
              }
            }
          }
        }
      }
    }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => actorSystem.terminate())
  }

}
