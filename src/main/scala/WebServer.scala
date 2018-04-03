import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import java.util
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.google.gson.Gson
import models.{Order, TickerData}
import mt4commands.MT4Commands
import pdi.jwt.{Jwt, JwtAlgorithm}
import sources.TickerSource
import specs.OrderSpec

import scala.concurrent.Future
import scala.io.StdIn

trait CsvParameters {
  implicit def csvSeqParamMarshaller: FromStringUnmarshaller[Seq[String]] =
    Unmarshaller(ex => s => Future.successful(s.split(",")))

  implicit def csvListParamMarshaller: FromStringUnmarshaller[List[String]] =
    Unmarshaller(ex => s => Future.successful(s.split(",").toList))
}

object CsvParameters extends CsvParameters

/**
  * Created by spectrum on 3/27/2018.
  */
object WebServer {
  def main(args: Array[String]) {
    implicit val actorSystem = ActorSystem("MT4Server")
    implicit val materializer = ActorMaterializer()
    implicit val executionCtx = actorSystem.dispatcher

    val password = "a72zkPP".toCharArray
    val API_KEY = "280EC427-5184-4AB4-8A12-EF4140057437"
    val secret_key = "I shot the sheriff"

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = getClass.getResourceAsStream("/cert.p12")

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)

    val route = {
      pathSingleSlash {
        complete("It's alive!!!")
      } ~
      pathPrefix("login") {
        pathEnd {
          post {
            entity(as[String])
            { apiKey =>
              {
                if (apiKey.equals(API_KEY)) {
                  val token = Jwt.encode(apiKey, secret_key, JwtAlgorithm.HS512)
                  complete(token)
                } else
                complete(StatusCodes.InternalServerError)
              }
            }
          }
        }
      } ~
      authorize(rc => {
        val header = rc.request.getHeader("Authorization");
        if(header.isPresent) Jwt.isValid(header.get().value(), secret_key, Seq(JwtAlgorithm.HS512)) else false
        true
      }) {
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
                      import CsvParameters._

                      parameters('tickers.as[List[String]].?) {
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
                } ~
              path("ticker") {
                extractUpgradeToWebSocket {
                  upgrade => {
                    parameters('ticker.as[String]) {
                      ticker => {
                        complete({
                          val tickerGraph = new TickerSource(exchangeId, ticker)
                          val tickers = Source.fromGraph(tickerGraph).map(e => {
                            TextMessage(e.toString)
                          })

                          upgrade.handleMessagesWithSinkSource(Sink.ignore, tickers)
                        })
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 443, connectionContext = https)

    println(s"Server online at http://localhost:443/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => actorSystem.terminate())
  }
}
