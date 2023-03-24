package snow

import utest.*
import scoin.PrivateKey
import scodec.bits.ByteVector
import io.circe.syntax.*
import io.circe.parser.decode

object FilterTest extends TestSuite {
  val tests = Tests {
    test("decode and encode filters") {
      val filterj =
        """{"authors": ["3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"], "#e": ["4bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"]}"""
      val dec = decode[Filter](filterj)
      assert(dec.isRight)

      val filter = dec.toTry.get
      filter.authors(
        0
      ) ==> "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
      filter.tags("e")(
        0
      ) ==> "4bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"

      val filterj2 = filter.asJson
      assert(
        filterj2.hcursor
          .get[List[String]]("#e")
          .toTry
          .get(0) == "4bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
      )
      assert(filterj2.hcursor.get[List[Int]]("kinds").isLeft)
    }

    test("event matching") {
      val event = Event(
        1,
        "nada",
        List(
          List(
            "e",
            "4bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d",
            "wss://relay.com"
          ),
          List(
            "p",
            "feefc63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
          ),
          List(
            "p",
            "a098c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
          )
        ),
        123456789
      ).sign(
        PrivateKey(
          ByteVector.fromValidHex(
            "7708c95f09947507c1044e8f48bcf6350aa6bff1507dd4acfc755b9239b5c962"
          )
        )
      )

      assert(
        Filter(ids =
          List(
            "407819c737d33b4fdc6b95a2a1d0d5fd7b4d642ab60b02b683aa7e9020500bd4"
          )
        ).matches(event)
      )

      assert(
        !Filter(ids =
          List(
            "507819c737d33b4fdc6b95a2a1d0d5fd7b4d642ab60b02b683aa7e9020500bd4"
          )
        ).matches(event)
      )

      assert(
        Filter(
          authors = List(
            "1d02cd6628a159d3817bfca98787189a332ea2edc0e3633236fcb7161bdf173e",
            "2d02cd6628a159d3817bfca98787189a332ea2edc0e3633236fcb7161bdf173e",
            "3d02cd6628a159d3817bfca98787189a332ea2edc0e3633236fcb7161bdf173e",
            "4d02cd6628a159d3817bfca98787189a332ea2edc0e3633236fcb7161bdf173e"
          ),
          tags = Map(
            "p" -> List(
              "a098c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
            )
          )
        ).matches(event)
      )
    }
  }
}
