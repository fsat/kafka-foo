package test.stream

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString

object GroupByLineBreaks {
  def apply(): GroupByLineBreaks = new GroupByLineBreaks
}

class GroupByLineBreaks extends GraphStage[FlowShape[ByteString, ByteString]] {
  val LineBreak = '\n'

  private var buffer = ByteString.empty

  private val in = Inlet[ByteString]("ReadUntilLineBreak.in")
  private val out = Outlet[ByteString]("ReadUntilLineBreak.out")

  override val shape: FlowShape[ByteString, ByteString] = FlowShape.of(in, out)


  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val dataBytes = grab(in)
          val idx = dataBytes.indexOf(LineBreak)
          if (idx != -1) {
            val (current, nextLine) = dataBytes.splitAt(idx)
            val currentLine = buffer.concat(current)

            push(out, currentLine)
            buffer = nextLine.drop(1)
          } else {
            buffer = buffer.concat(dataBytes)
            pull(in)
          }
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = pull(in)
      })
    }
}

