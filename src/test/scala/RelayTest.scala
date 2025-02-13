package snow

import utest.*
import scala.concurrent.duration.*
import scala.scalajs
import cats.implicits.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import org.http4s.syntax.literals.uri
import fs2.concurrent.Channel

object RelayTest extends TestSuite {

  /** Note: this is a pretty poorly written test and surely can/should be
    * cleaned up. However, when running this test with `debugOn=true` passed to
    * `Relay.mkResourceForIO` it can be seen in the output that the test:
    * \- creates two subscriptions:
    * \1. a subscription for kind 0 events (internally this is given subid 0) 2.
    * a suscription for kind 1 events (internally this is given subid 1)
    * \- the correct events are sent to and received by the correct subscription
    * \- no events are skipped/lost
    */
  val tests = Tests {
    test("connect to relay and subscribe") {
      val numStoredEvents = 3
      val program =
        Relay
          .mkResourceForIO(uri"wss://relay.damus.io", debugOn = true)
          .flatMap { relay =>
            (
              relay.subscribe(
                Filter(kinds = List(0), limit = Some(numStoredEvents))
              ),
              relay.subscribe(
                Filter(kinds = List(1), limit = Some(numStoredEvents))
              )
            ).parTupled
          }
          .use { case ((stored, live), (stored2, live2)) =>
            stored.traverse(e => IO.println((e.kind, e.hash))) *>
              IO.println(s"done processing ${stored.size} stored events") *>
              IO.delay {
                assert(stored.size == numStoredEvents)
              } *> IO.println(
                "now processing live stream of events (stopping after 1)"
              ) *>
              live
                .take(1)
                .evalTap(e => IO.println((e.kind, e.hash)))
                .compile
                .drain
              *> stored2.traverse(e => IO.println((e.kind, e.hash))) *>
              IO.println(s"done processing ${stored.size} stored2 events") *>
              IO.delay {
                assert(stored2.size == numStoredEvents)
              } *> IO.println(
                "now processing live stream2 of events (stopping after 1)"
              ) *>
              live2
                .take(1)
                .evalTap(e => IO.println((e.kind, e.hash)))
                .compile
                .drain
          }
      program.unsafeToFuture()
    }

  }
}
