package test.actor

import akka.stream.scaladsl.Sink
import akka.testkit.TestProbe
import akka.util.ByteString
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalatest.Inside
import test.UnitTestLike

class KafkaPublishTest extends UnitTestLike with Inside {
  "kafka actor publisher" should {
    "publish messages to kafka" in {
      val testProbe = TestProbe()

      val publisher = actorSystem.actorOf(KafkaPublish.props[Array[Byte], String](Sink.foreach(testProbe.ref ! _)))

      publisher ! KafkaPublish.Publish("topic-1", ByteString("hello"))
      testProbe.expectMsg(new ProducerRecord[Array[Byte], String]("topic-1", "hello"))

      publisher ! KafkaPublish.Publish("topic-2", ByteString("test"))
      testProbe.expectMsg(new ProducerRecord[Array[Byte], String]("topic-2", "test"))
    }
  }
}
