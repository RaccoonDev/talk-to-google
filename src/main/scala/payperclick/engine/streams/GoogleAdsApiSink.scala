package payperclick.engine.streams

import akka.stream.{Attributes, FlowShape, Inlet, Outlet, SinkShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

case class BiddingRequest(accountId: Long, adGroupId: Long, keywordId: Long, maxCpc: Double)
case class BiddingResults(request: BiddingRequest, isSuccess: Boolean, errorMessage: String)

trait GoogleAdsApi {
  def sendBidsBatch(req: Seq[BiddingRequest]): Seq[BiddingResults]
}

class BiddingRequestsFlow(api: GoogleAdsApi) extends GraphStage[FlowShape[Seq[BiddingRequest],Seq[BiddingResults]]] {

  val inPort: Inlet[Seq[BiddingRequest]] = Inlet[Seq[BiddingRequest]]("biddingRequests")
  val outPort: Outlet[Seq[BiddingResults]] = Outlet[Seq[BiddingResults]]("biddingResults")

  override def shape: FlowShape[Seq[BiddingRequest], Seq[BiddingResults]] =
    FlowShape[Seq[BiddingRequest], Seq[BiddingResults]](inPort, outPort)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    // mutable state

    setHandler(inPort, new InHandler {
      override def onPush(): Unit = {
        try {
          val bids = grab(inPort)
          val results = api.sendBidsBatch(bids)
          push(outPort, results)
        } catch {
          case e: Throwable => failStage(e)
        }
      }
    })

    setHandler(outPort, new OutHandler {
      override def onPull(): Unit = {
        pull(inPort)
      }
    })

  }
}

