package snow

import utest.*
import io.circe.syntax.*
import io.circe.parser.{parse, decode}
import scodec.bits.ByteVector
import scoin.PrivateKey
import scoin.XOnlyPublicKey

object NIP19Test extends TestSuite {
  val tests = Tests {
    test("decode npub") {
      val value = NIP19
        .decode(
          "npub148ut8u4vr8xqd4gefhg6eyc5636p5zthw3zfse2njfkezegczers59ty0w"
        )
        .toTry
        .get

      assertMatch(value) { case _: XOnlyPublicKey => }

      value match {
        case pk: XOnlyPublicKey =>
          pk.value.toHex ==> "a9f8b3f2ac19cc06d5194dd1ac9314d4741a09777444986553926d9165181647"
        case _ =>
      }
    }

    test("decode nsec") {
      val value = NIP19
        .decode(
          "nsec1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqsmhltgl"
        )
        .toTry
        .get

      assertMatch(value) { case _: PrivateKey => }

      value match {
        case sk: PrivateKey =>
          sk.value.toHex ==> "0000000000000000000000000000000000000000000000000000000000000001"
        case _ =>
      }
    }

    test("decode note") {
      val value = NIP19
        .decode(
          "note1yw5agdtgrkwytpy2ch9pahfewhf9dyr2zkl56gjqrf4hnz2wc3lqj8w45q"
        )
        .toTry
        .get

      assertMatch(value) { case _: String => }

      value match {
        case id: String =>
          id ==> "23a9d435681d9c45848ac5ca1edd3975d256906a15bf4d22401a6b79894ec47e"
        case _ =>
      }
    }

    test("decode nevent") {
      val value = NIP19
        .decode(
          "nevent1qqsz82w5x45pm8z9sj9vtjs7m5uht5jkjp4pt06dyfqp56me398vglspp3mhxue69uhhstnrdakj7q3q9klqjtr2mgfk0m9h8g80z8xjcv07kv340qjvvjsclrdgt93pf4cqur7gsc"
        )
        .toTry
        .get

      assertMatch(value) { case _: EventPointer => }

      value match {
        case evp: EventPointer =>
          evp.id ==> "23a9d435681d9c45848ac5ca1edd3975d256906a15bf4d22401a6b79894ec47e"
          evp.relays ==> List("wss://x.com/")
          evp.author.map(_.value.toHex) ==> Some(
            "2dbe092c6ada1367ecb73a0ef11cd2c31feb32357824c64a18f8da8596214d70"
          )
        case _ =>
      }
    }

    test("decode nprofile") {
      val value = NIP19
        .decode(
          "nprofile1qqsw96tn6z4zpgs24enrec7zak9mzcdekt0edf08vrfenln8t4m5v8sppdmhxue69uhhjtnrdakszrrhwden5te00qhxxmmd9um2rgu3"
        )
        .toTry
        .get

      assertMatch(value) { case _: ProfilePointer => }

      value match {
        case pp: ProfilePointer =>
          pp.pubkey.value.toHex ==> "e2e973d0aa20a20aae663ce3c2ed8bb161b9b2df96a5e760d399fe675d77461e"
          pp.relays.size ==> 2
          pp.relays.contains("wss://x.com/")
          pp.relays.contains("wss://y.com/")
        case _ =>
      }
    }

    test("decode naddr") {
      val value = NIP19
        .decode(
          "naddr1qqrxyctwv9hxzq3qut5h8592yz3q4tnx8n3u9mvtk9smnvklj6j7wcxnn8lxwhthgc0qxpqqqzgauhfurwa"
        )
        .toTry
        .get

      assertMatch(value) { case _: Address => }

      value match {
        case addr: Address =>
          addr.author.value.toHex ==> "e2e973d0aa20a20aae663ce3c2ed8bb161b9b2df96a5e760d399fe675d77461e"
          addr.relays.size ==> 0
          addr.kind ==> 37342
          addr.d ==> "banana"
        case _ =>
      }
    }
  }
}
