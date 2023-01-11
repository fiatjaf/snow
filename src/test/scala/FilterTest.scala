package snow

import utest._
import io.circe.parser.decode

object FilterTest extends TestSuite {
  val tests = Tests {
    test("decode and encode filters") {
      val filterj =
        """{"authors": ["3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"], "#e": ["4bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"]}"""
      val dec = decode[Filter](filterj)
      assert(dec.isRight)

      val filter = dec.toTry.get
      filter.authors.get(
        0
      ) ==> "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
      filter.tags("e")(
        0
      ) ==> "4bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
    }
  }
}
