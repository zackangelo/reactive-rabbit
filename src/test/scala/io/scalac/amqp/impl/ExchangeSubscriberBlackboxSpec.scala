package io.scalac.amqp.impl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.scalac.amqp.{Connection, Message, Routed}
import org.reactivestreams.tck.{SubscriberBlackboxVerification, TestEnvironment}
import org.scalatest.testng.TestNGSuiteLike
import org.testng.annotations.AfterSuite

import scala.concurrent.duration._

class ExchangeSubscriberBlackboxSpec(defaultTimeout: FiniteDuration) extends SubscriberBlackboxVerification[Routed](
  new TestEnvironment(defaultTimeout.toMillis)) with TestNGSuiteLike {

  def this() = this(300.millis)

  val connection = Connection()
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  @AfterSuite def cleanup() = system.shutdown()

  override def createSubscriber() = connection.publish("nowhere")

  val message = Routed(routingKey = "foo", message = Message())

  def createHelperSource(elements: Long): Source[Routed, Unit] = elements match {
    /** if `elements` is 0 the `Publisher` should signal `onComplete` immediately. */
    case 0                      ⇒ Source.empty
    /** if `elements` is [[Long.MaxValue]] the produced stream must be infinite. */
    case Long.MaxValue          ⇒ Source(() ⇒ Iterator.continually(message))
    /** It must create a `Publisher` for a stream with exactly the given number of elements. */
    case n if n <= Int.MaxValue ⇒ Source(List.fill(n.toInt)(message))
    /** I assume that the number of elements is always less or equal to [[Int.MaxValue]] */
    case n                      ⇒ sys.error("n > Int.MaxValue")
  }

  override def createHelperPublisher(elements: Long) = createHelperSource(elements).runWith(Sink.publisher)
  override def createElement(element: Int) = message
}
