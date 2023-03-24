package snow

import java.util.Date
import scala.util.Try
import scoin.{Crypto, PrivateKey, XOnlyPublicKey, ByteVector32, ByteVector64}
import scodec.bits.ByteVector
import cats.syntax.all.*
import io.circe.*
import io.circe.syntax.*

object Event {
  given Encoder[Event] = new Encoder[Event] {
    final def apply(evt: Event): Json = {
      var jo = JsonObject(
        "created_at" := evt.created_at,
        "kind" := evt.kind,
        "tags" := evt.tags,
        "content" := evt.content
      )

      evt.id.foreach { id =>
        jo = jo.add("id", evt.id.getOrElse("").asJson)
      }

      evt.pubkey.foreach { pubkey =>
        jo = jo.add("pubkey", evt.pubkey.map(_.toHex).getOrElse("").asJson)
      }

      evt.sig.foreach { sig =>
        jo = jo.add("sig", evt.sig.map(_.toHex).getOrElse("").asJson)
      }

      jo.asJson
    }
  }

  given Decoder[Event] = new Decoder[Event] {
    final def apply(c: HCursor): Decoder.Result[Event] = {
      (
        c.downField("kind").as[Int],
        c.downField("content").as[String],
        c.downField("tags").as[List[List[String]]],
        c.downField("created_at").as[Long],
        c
          .downField("pubkey")
          .as[String]
          .flatMap[DecodingFailure, XOnlyPublicKey](hex =>
            ByteVector
              .fromHex(hex)
              .filter(_.size == 32)
              .map(b => XOnlyPublicKey(ByteVector32(b)))
              .toRight(
                DecodingFailure(
                  DecodingFailure.Reason
                    .CustomReason("pubkey is not 32 bytes valid hex"),
                  List.empty
                )
              )
          )
          .map(Some(_))
          .recoverWith(_ => Right(None)),
        c
          .downField("id")
          .as[String]
          .map(Some(_))
          .recoverWith(_ => Right(None)),
        c
          .downField("sig")
          .as[String]
          .flatMap(hex =>
            ByteVector
              .fromHex(hex)
              .filter(_.size == 64)
              .map(ByteVector64(_))
              .toRight(
                DecodingFailure(
                  DecodingFailure.Reason
                    .CustomReason("signature is not 64 bytes hex"),
                  List.empty
                )
              )
          )
          .map(Some(_))
          .recoverWith(_ => Right(None))
      )
        .mapN(Event.apply)
    }
  }
}

case class Event(
    kind: Int,
    content: String,
    tags: List[List[String]] = List.empty,
    created_at: Long = new Date().getTime() / 1000,
    pubkey: Option[XOnlyPublicKey] = None,
    id: Option[String] = None,
    sig: Option[ByteVector64] = None
) {
  lazy val serialized: String =
    List[Json](
      0.asJson,
      pubkey.map(_.toHex).getOrElse("").asJson,
      created_at.asJson,
      kind.asJson,
      tags.asJson,
      content.asJson
    ).asJson.noSpaces

  lazy val hash: ByteVector32 =
    Crypto.sha256(ByteVector.encodeUtf8(serialized).toOption.get)

  def sign(privateKey: PrivateKey): Event = {
    val event = this.copy(pubkey = Some(privateKey.publicKey.xonly))
    event.copy(
      id = Some(event.hash.toHex),
      sig = Some(Crypto.signSchnorr(event.hash, privateKey))
    )
  }

  def isValid: Boolean =
    id == Some(hash.toHex) && (for {
      pk <- pubkey
      sig64 <- sig
    } yield Crypto.verifySignatureSchnorr(sig64, hash, pk)).getOrElse(false)

  def getTagValues(key: String): List[String] =
    tags.filter(items => items.size >= 2 && items(0) == key).map(_(1))
}
