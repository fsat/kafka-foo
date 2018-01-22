package test.actor

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import org.apache.kafka.clients.producer.ProducerRecord
import test.actor.KafkaPublish.{ConnectToKafka, Publish}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.Failure

object KafkaPublish {
  def props[K, V](publishToKafka: Sink[ProducerRecord[K, V], Future[Done]]): Props = Props(new KafkaPublish(publishToKafka))

  sealed trait Message

  object Publish {
    def toProducerRecord(input: Publish): ProducerRecord[Array[Byte], String] =
      new ProducerRecord[Array[Byte], String](input.topic, input.message.utf8String)
  }

  case class Publish(topic: String, message: ByteString)

  sealed trait InternalMessage extends Message
  case object ConnectToKafka extends InternalMessage
}

class KafkaPublish[K, V](publishToKafka: Sink[ProducerRecord[K, V], Future[Done]]) extends Actor with ActorLogging {
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import context.dispatcher

  override def preStart(): Unit = {
    self ! ConnectToKafka
  }

  override def receive: Receive = connecting(Seq.empty)

  private def connecting(cachedMessages: Seq[KafkaPublish.Publish]): Receive = {
    case ConnectToKafka =>
      val (ref, stream) = Source.actorRef(1024, OverflowStrategy.dropHead)
        .toMat(publishToKafka)(Keep.both)
        .run()

      log.info("Connected to Kafka")
      context.become(connected(ref))

      cachedMessages.foreach(v => ref ! Publish.toProducerRecord(v))

      val myself = context.self
      stream.onComplete {
        case Failure(e) =>
          log.error(e, "Disconnected from Kafka - reconnecting...")
          context.become(connecting(Seq.empty))
          myself ! ConnectToKafka
        case _ =>
          log.error("Disconnected from Kafka - reconnecting...")
          context.become(connecting(Seq.empty))
          myself ! ConnectToKafka
      }

    case v: Publish =>
      context.become(connecting(cachedMessages :+ v))
  }

  private def connected(ref: ActorRef): Receive = {
    case v: Publish =>
      ref ! Publish.toProducerRecord(v)
  }
}
