package test

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, Matchers, Suite, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

trait UnitTestLike extends WordSpec with BeforeAndAfterAll with Matchers {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  override protected def afterAll(): Unit = {
    Await.result(actorSystem.terminate(), 3.seconds)
  }
}
