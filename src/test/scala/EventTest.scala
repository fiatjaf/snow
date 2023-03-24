package snow

import utest.*
import io.circe.syntax.*
import io.circe.parser.{parse, decode}
import scodec.bits.ByteVector
import scoin.PrivateKey

object EventTest extends TestSuite {
  val tests = Tests {
    test("decode event") {
      val evtj =
        """{"id":"dc90c95f09947507c1044e8f48bcf6350aa6bff1507dd4acfc755b9239b5c962","pubkey":"3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d","created_at":1644271588,"kind":1,"tags":[],"content":"now that https://blueskyweb.org/blog/2-7-2022-overview was announced we can stop working on nostr?","sig":"230e9d8f0ddaf7eb70b5f7741ccfa37e87a455c9a469282e3464e2052d3192cd63a167e196e381ef9d7e69e9ea43af2443b839974dc85d8aaab9efe1d9296524"}"""
      val dec = decode[Event](evtj)
      assert(dec.isRight)

      val event = dec.toTry.get
      assert(event.isValid)

      event.kind ==> 1
    }

    test("sign and encode event") {
      val event = Event(1, "hello hello", created_at = 1234567).sign(
        PrivateKey(
          ByteVector.fromValidHex(
            "7708c95f09947507c1044e8f48bcf6350aa6bff1507dd4acfc755b9239b5c962"
          )
        )
      )

      assert(event.isValid)
      assert(event.created_at == 1234567)

      val evtj = event.asJson.noSpaces
      assert(evtj.startsWith("""{"""))

      val evt = parse(evtj).toTry.get
      assert(evt.hcursor.get[String]("content") == Right("hello hello"))
      assert(
        evt.hcursor.get[String]("pubkey") == Right(
          "4d02cd6628a159d3817bfca98787189a332ea2edc0e3633236fcb7161bdf173e"
        )
      )
    }
  }
}
