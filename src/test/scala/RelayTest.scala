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
  scalajs.js.Dynamic.global.globalThis.require("websocket-polyfill")

  val tests = Tests {
    test("connect to relay and subscribe") {
      val program = Relay(uri"wss://nostr.fmt.wiz.biz").use { relay =>
        relay
          .subscribe(
            Filter(kinds = List(1), limit = Some(5))
          )
          .flatMap { (stored, live) =>
            IO.delay {
              println(stored.size)
              assert(stored.size == 5)
            } *>
              live
                .evalTap { evt =>
                  IO.delay {
                    assert(evt.kind == 0)
                  }
                }
                .take(1)
                .compile
                .drain
          }
      }

      program.unsafeToFuture()
    }
  }
}
