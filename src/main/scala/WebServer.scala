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
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
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

final object CsvParameters extends CsvParameters

/**
  * Created by spectrum on 3/27/2018.
  */
class WebServer
final object WebServer {
  val logger = Logger[WebServer]

  def main(args: Array[String]) {
    implicit val actorSystem = ActorSystem("MT4Server")
    implicit val materializer = ActorMaterializer()
    implicit val executionCtx = actorSystem.dispatcher

    val conf = ConfigFactory.load()

    val keystore_password = conf.getString("keystore_password").toCharArray
    val API_KEY = conf.getString("api_key")
    val secret_key = conf.getString("secret_key")

    val ks: KeyStore = KeyStore.getInstance("PKCS12")

    val keystore: InputStream = getClass.getResourceAsStream("/cert.p12")
    require(keystore != null, "Keystore required!")
    ks.load(keystore, keystore_password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, keystore_password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)

    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)

    val route = {
      pathSingleSlash {
        complete("It's alive!!!")
      } ~
      pathPrefix("token") {
        pathEnd {
          post {
            entity(as[String])
            { apiKey =>
              {
                logger.info("Commencing login")
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
      }) {
        pathPrefix("exchanges") {
          pathPrefix(Segment) {
            exchangeId => {
              pathPrefix("orders") {
                pathEnd {
                  get {
                    logger.info(s"Commencing get orders for exchange ${exchangeId}")

                    val ordersList = new util.ArrayList[Order]()
                    MT4Commands(exchangeId).getOrders foreach ordersList.add

                    complete(new Gson().toJson(ordersList))
                  }
                } ~
                  post {
                    entity(as[String]) {
                      orderSpecJson => {
                        logger.info(s"Commencing create order ${orderSpecJson} for exchange ${exchangeId}")

                        val orderSpec = new Gson().fromJson(orderSpecJson, classOf[OrderSpec])
                        complete({
                          MT4Commands(exchangeId).placeOrder(orderSpec).toString
                        })
                      }
                    }
                  } ~
                  pathPrefix(IntNumber) {
                    orderId => {
                      pathEnd {
                        delete {
                          logger.info(s"Commencing delete order ${orderId} for exchange ${exchangeId}")

                          complete(MT4Commands(exchangeId).deleteOrder(orderId).toString)
                        }
                      }
                    }
                  }
              } ~
                pathPrefix("balance") {
                  pathEnd {
                    get {
                      logger.info(s"Commencing get balance for exchange ${exchangeId}")

                      complete(MT4Commands(exchangeId).balance.toString)
                    }
                  }
                } ~
                pathPrefix("tickers") {
                  get {
                    import CsvParameters._

                    parameters('tickers.as[List[String]].?) {
                      tickers => {
                        logger.info(s"Commencing get tickers ${tickers} for exchange ${exchangeId}")

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
                        logger.info(s"Commencing get ticker ${ticker} via socket for exchange ${exchangeId}")

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

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 443, connectionContext = https)

    logger.info(s"Server online at http://0.0.0.0:443/\nPress RETURN to stop...")
    StdIn.readLine

    keystore.close
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => actorSystem.terminate())
  }
}
