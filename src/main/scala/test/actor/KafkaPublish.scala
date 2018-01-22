package test.actor

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import org.apache.kafka.clients.producer.ProducerRecord
import test.actor.KafkaPublish.{ConnectToKafka, Disconnected, Publish}

import scala.collection.immutable.Seq
import scala.concurrent.Future

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
  case class Disconnected(error: Option[Throwable]) extends InternalMessage
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

      stream
        .map(_ => Disconnected(error = None))
        .recover {
          case e => Disconnected(error = Some(e))
        }
        .pipeTo(context.self)

    case v: Publish =>
      context.become(connecting(cachedMessages :+ v))

    case Disconnected(error) =>
      error.fold(log.error("Disconnected from Kafka, reconnecting..."))(log.error(_, "Disconnected from Kafka, reconnecting..."))
      self ! ConnectToKafka
  }

  private def connected(ref: ActorRef): Receive = {
    case v: Publish =>
      ref ! Publish.toProducerRecord(v)

    case Disconnected(error) =>
      error.fold(log.error("Disconnected from Kafka, reconnecting..."))(log.error(_, "Disconnected from Kafka, reconnecting..."))
      context.become(connecting(Seq.empty))
      self ! ConnectToKafka
  }
}
