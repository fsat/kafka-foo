package test

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream._
import akka.stream.scaladsl.StreamConverters
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import test.stream.{GroupByLineBreaks, TerminateWhenExit}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Failure

object Simple {

  def main(args: Array[String]): Unit = {
    println("Reading from STDIN. Type exit to terminate.")

    implicit val actorSystem: ActorSystem = ActorSystem("kafka-foo")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    import actorSystem.dispatcher

    val producerSettings = ProducerSettings(actorSystem, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")


    val stream = StreamConverters
      .fromInputStream(() => System.in, 1)
      .via(GroupByLineBreaks())
      .via(TerminateWhenExit())
      .map(text => new ProducerRecord[Array[Byte], String]("test", text.utf8String))
      .runWith(Producer.plainSink(producerSettings))


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
