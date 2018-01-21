package test

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString

object TerminateWhenExit {
  def apply(): TerminateWhenExit = new TerminateWhenExit
}

class TerminateWhenExit extends GraphStage[FlowShape[ByteString, ByteString]] {
  private val in = Inlet[ByteString]("TerminateWhenExit.in")
  private val out = Outlet[ByteString]("TerminateWhenExit.out")

  override val shape: FlowShape[ByteString, ByteString] = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val dataBytes = grab(in)
          if (dataBytes.utf8String == "exit")
            completeStage()
          else
            push(out, dataBytes)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
      })
    }
}
