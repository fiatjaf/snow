package snow

import scala.util.Try
import scoin.{Crypto, ByteVector32, ByteVector64}
import scodec.bits.ByteVector
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

object Event {
  given Decoder[Event] = deriveDecoder[Event]
  given Encoder[Event] = deriveEncoder[Event]
}

case class Event(
    id: String,
    pubkey: String,
    created_at: Long,
    kind: Int,
    tags: List[List[String]],
    content: String,
    sig: String
) {
  lazy val serialized: String = List[Json](
    0.asJson,
    pubkey.asJson,
    created_at.asJson,
    kind.asJson,
    tags.asJson,
    content.asJson
  ).asJson.noSpaces

  lazy val hash: ByteVector32 =
    Crypto.sha256(ByteVector.encodeUtf8(serialized).toOption.get)

  def sign(privateKey: Crypto.PrivateKey): Event = this.copy(
    id = hash.toHex,
    pubkey = privateKey.publicKey.xonly.value.toHex,
    sig = Crypto.signSchnorr(hash, privateKey).toHex
  )

  def isValid: Boolean =
    id == hash.toHex && (for {
      pkb <- ByteVector.fromHex(pubkey)
      pk32 <- Try(ByteVector32(pkb)).toOption
      pk = Crypto.XOnlyPublicKey(pk32)
      sigb <- ByteVector.fromHex(sig)
      sig64 <- Try(ByteVector64(sigb)).toOption
    } yield Crypto.verifySignatureSchnorr(hash, sig64, pk)).getOrElse(false)
}
