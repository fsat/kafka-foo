package test

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.StreamConverters
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import test.actor.KafkaPublish
import test.stream.{GroupByLineBreaks, TerminateWhenExit}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Failure

object ViaActor {
  def main(args: Array[String]): Unit = {
    println("Reading from STDIN. Type exit to terminate.")

    implicit val actorSystem: ActorSystem = ActorSystem("kafka-foo")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    import actorSystem.dispatcher

    val producerSettings = ProducerSettings(actorSystem, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")

    val kafkaPublish = actorSystem.actorOf(KafkaPublish.props(Producer.plainSink(producerSettings)), "kafka-publish")

    val stream = StreamConverters
      .fromInputStream(() => System.in, 1)
      .via(GroupByLineBreaks())
      .via(TerminateWhenExit())
      .runForeach(v => kafkaPublish ! KafkaPublish.Publish(topic = "test", v))


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
