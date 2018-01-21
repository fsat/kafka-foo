package test

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.StreamConverters

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Failure

object Main {

  def main(args: Array[String]): Unit = {
    println("Starting STDIN streams")

    implicit val actorSystem: ActorSystem = ActorSystem("kafka-foo")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    import actorSystem.dispatcher

    val stream = StreamConverters
      .fromInputStream(() => System.in, 1)
      .via(GroupByLineBreaks())
      .via(TerminateWhenExit())
      .runForeach(v => println(v.utf8String))

    stream.onComplete {
      case Failure(e) =>
        e.printStackTrace()
      case _ =>
    }

    Await.result(stream, Duration.Inf)

    println("Shutting down actor system...")
    Await.ready(actorSystem.terminate(), Duration.Inf)
    println("DONE")
  }
}
