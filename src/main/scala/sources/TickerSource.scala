package sources

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.stage.{GraphStage, GraphStageLogic}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.google.gson.Gson
import com.typesafe.scalalogging.Logger
import mt4commands.MT4Commands

/**
  * Created by spectrum on 4/2/2018.
  */
class TickerSource(exchangeId: String, ticker: String) extends GraphStage[SourceShape[Message]] {
  val out: Outlet[Message] = Outlet("TickerData")
  val logger = Logger[TickerSource]

  override def shape = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes) = {
    new GraphStageLogic(shape) {
      var tickerJsonOld = ""
      setHandler(out, () => {
        Thread.sleep(10)
        val tickerJson = new Gson().toJson(MT4Commands(exchangeId).getSingleTickerData(ticker))
        if (!(tickerJsonOld eq tickerJson)){
          tickerJsonOld = tickerJson
          emit(out, TextMessage(tickerJson))
        }
      })
    }
  }
}
