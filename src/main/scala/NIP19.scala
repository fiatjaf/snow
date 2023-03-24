package snow

import scala.util.{Try, Success, Failure}
import scodec.bits.*
import scoin.{PrivateKey, XOnlyPublicKey, Bech32, ByteVector32}

case class EventPointer(
    id: String,
    relays: List[String] = List.empty,
    author: Option[XOnlyPublicKey] = None
)

case class ProfilePointer(
    pubkey: XOnlyPublicKey,
    relays: List[String] = List.empty
)

case class Address(
    d: String,
    kind: Int,
    author: XOnlyPublicKey,
    relays: List[String]
)

enum TLVType:
  case Special
  case Relays
  case Author
  case Kind
  case Unknown

object TLVType {
  def fromByte(b: ByteVector): TLVType = fromByte(b(0))

  def fromByte(b: Byte): TLVType = b match {
    case 0 => TLVType.Special
    case 1 => TLVType.Relays
    case 2 => TLVType.Author
    case 3 => TLVType.Kind
    case _ => TLVType.Unknown
  }
}

object NIP19 {
  def decode(bech32text: String): Either[
    Throwable,
    PrivateKey | EventPointer | ProfilePointer | Address,
  ] =
    Try(Bech32.decode(bech32text)) match {
      case Failure(err) => Left(err)
      case Success((_, _, enc)) if enc == Bech32.Bech32mEncoding =>
        Left(Error("encoding is in bech32, not bech32m"))
      case Success((prefix, data5, _)) =>
        val data = Bech32.five2eight(data5)
        prefix match {
          case "npub"     => parseNpub(data)
          case "nsec"     => parseNsec(data)
          case "note"     => parseNote(data)
          case "nprofile" => parseNprofile(data)
          case "nevent"   => parseNevent(data)
          case "naddr"    => parseNaddr(data)
          case _          => Left(Error(s"unsupported prefix '$prefix'"))
        }
    }

  def parseNpub(data: Array[Byte]): Either[Throwable, ProfilePointer] =
    if data.size == 32 then
      Right(
        ProfilePointer(pubkey = XOnlyPublicKey(ByteVector32(ByteVector(data))))
      )
    else Left(Error("npub must contain 32 bytes"))

  def parseNsec(data: Array[Byte]): Either[Throwable, PrivateKey] =
    if data.size == 32 then Right(PrivateKey(ByteVector32(ByteVector(data))))
    else Left(Error("nsec must contain 32 bytes"))

  def parseNote(data: Array[Byte]): Either[Throwable, EventPointer] =
    if data.size == 32 then Right(EventPointer(id = ByteVector(data).toHex))
    else Left(Error("note must contain 32 bytes"))

  def parseNprofile(data: Array[Byte]): Either[Throwable, ProfilePointer] = {
    var pubkey: XOnlyPublicKey = null
    var relays: List[String] = List.empty
    parseTLV(data).foreach {
      case TLVRecord(TLVType.Special, v) =>
        if (v.size == 32) {
          pubkey = XOnlyPublicKey(ByteVector32(v))
        }
      case TLVRecord(TLVType.Relays, v) =>
        relays = v.decodeUtf8Lenient :: relays
      case _ =>
    }
    if pubkey != null then Right(ProfilePointer(pubkey, relays))
    else Left(Error("nprofile pubkey record missing or invalid"))
  }

  def parseNevent(data: Array[Byte]): Either[Throwable, EventPointer] = {
    var id: String = null
    var relays: List[String] = List.empty
    var author: Option[XOnlyPublicKey] = None
    parseTLV(data).foreach {
      case TLVRecord(TLVType.Special, v) =>
        id = v.toHex
      case TLVRecord(TLVType.Relays, v) =>
        relays = v.decodeUtf8Lenient :: relays
      case TLVRecord(TLVType.Author, v) =>
        if (v.size == 32) {
          author = Some(XOnlyPublicKey(ByteVector32(v)))
        }
      case _ =>
    }
    if id != null then Right(EventPointer(id, relays, author))
    else Left(Error("nevent id record missing or invalid"))
  }

  def parseNaddr(data: Array[Byte]): Either[Throwable, Address] =
    var d: String = null
    var relays: List[String] = List.empty
    var author: XOnlyPublicKey = null
    var kind: Int = -1
    parseTLV(data).foreach {
      case TLVRecord(TLVType.Special, v) =>
        d = v.decodeUtf8Lenient
      case TLVRecord(TLVType.Relays, v) =>
        relays = v.decodeUtf8Lenient :: relays
      case TLVRecord(TLVType.Author, v) =>
        if (v.size == 32) {
          author = XOnlyPublicKey(ByteVector32(v))
        }
      case TLVRecord(TLVType.Kind, v) =>
        kind = v.toInt(signed = false, ordering = ByteOrdering.BigEndian)
      case _ =>
    }
    if d == null then Left(Error("naddr d record missing or invalid"))
    else if author == null then
      Left(Error("naddr author record missing or invalid"))
    else if kind == -1 then Left(Error("naddr kind record missing or invalid"))
    else Right(Address(d, kind, author, relays))

  def parseTLV(data: Array[Byte]): List[TLVRecord] =
    parseTLVRecords(List.empty, ByteVector(data))

  def parseTLVRecords(
      records: List[TLVRecord],
      bytes: ByteVector
  ): List[TLVRecord] = {
    if (bytes.size == 0) records
    else
      parseTLVRecord(bytes)
        .map((rest, record) => parseTLVRecords(record :: records, rest))
        .getOrElse(List.empty)
  }

  def parseTLVRecord(
      bytes: ByteVector
  ): Either[String, (ByteVector, TLVRecord)] =
    bytes.consume(1)(b => Right(TLVType.fromByte(b))).flatMap { (rest, t) =>
      rest
        .consume(1)(b =>
          Right(
            b.toInt(signed = false, ordering = ByteOrdering.BigEndian)
          )
        )
        .flatMap { (rest, l) =>
          rest.consume(l)(b => Right(b)).map { (rest, v) =>
            (rest, TLVRecord(t, v))
          }
        }
    }

  case class TLVRecord(t: TLVType, value: ByteVector)
}
