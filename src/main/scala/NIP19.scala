package snow

import scala.util.{Try, Success, Failure}
import scodec.bits.*
import scoin.{PrivateKey, XOnlyPublicKey, Bech32, ByteVector32}
import scoin.Bech32.Bech32Encoding

case class EventPointer(
    id: String,
    relays: List[String] = List.empty,
    author: Option[XOnlyPublicKey] = None
)

case class ProfilePointer(
    pubkey: XOnlyPublicKey,
    relays: List[String] = List.empty
)

case class AddressPointer(
    d: String,
    kind: Int,
    author: XOnlyPublicKey,
    relays: List[String]
)

object NIP19 {
  import NIP19Decoder.*
  import NIP19Encoder.*

  def decode(bech32text: String): Either[
    Throwable,
    PrivateKey | EventPointer | ProfilePointer | AddressPointer,
  ] =
    Try(Bech32.decode(bech32text)) match {
      case Failure(err) => Left(err)
      case Success((_, _, enc)) if enc == Bech32.Bech32mEncoding =>
        Left(Error("encoding is in bech32, not bech32m"))
      case Success((prefix, data5, _)) =>
        val data = Bech32.five2eight(data5)
        prefix match {
          case "npub"     => decodeNpub(data)
          case "nsec"     => decodeNsec(data)
          case "note"     => decodeNote(data)
          case "nprofile" => decodeNprofile(data)
          case "nevent"   => decodeNevent(data)
          case "naddr"    => decodeNaddr(data)
          case _          => Left(Error(s"unsupported prefix '$prefix'"))
        }
    }

  def encode(
      thing: PrivateKey | XOnlyPublicKey | EventPointer | ProfilePointer |
        AddressPointer | ByteVector32
  ): String = {
    val (prefix, bytes) = thing match {
      case sk: PrivateKey       => encodeNsec(sk)
      case pp: ProfilePointer   => encodeNprofile(pp)
      case evp: EventPointer    => encodeNevent(evp)
      case addr: AddressPointer => encodeNaddr(addr)
      case pk: XOnlyPublicKey   => encodeNpub(pk)
      case id: ByteVector32     => encodeNote(id)
    }
    val bytes5 = Bech32.eight2five(bytes.toArray)
    Bech32.encode(prefix, bytes5, Bech32Encoding)
  }
}

object NIP19Decoder {
  import NIP19TLV.*

  def decodeNpub(data: Array[Byte]): Either[Throwable, ProfilePointer] =
    if data.size == 32 then
      Right(
        ProfilePointer(pubkey = XOnlyPublicKey(ByteVector32(ByteVector(data))))
      )
    else Left(Error("npub must contain 32 bytes"))

  def decodeNsec(data: Array[Byte]): Either[Throwable, PrivateKey] =
    if data.size == 32 then Right(PrivateKey(ByteVector32(ByteVector(data))))
    else Left(Error("nsec must contain 32 bytes"))

  def decodeNote(data: Array[Byte]): Either[Throwable, EventPointer] =
    if data.size == 32 then Right(EventPointer(id = ByteVector(data).toHex))
    else Left(Error("note must contain 32 bytes"))

  def decodeNprofile(data: Array[Byte]): Either[Throwable, ProfilePointer] = {
    var pubkey: XOnlyPublicKey = null
    var relays: List[String] = List.empty
    decodeTLV(data).foreach {
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

  def decodeNevent(data: Array[Byte]): Either[Throwable, EventPointer] = {
    var id: String = null
    var relays: List[String] = List.empty
    var author: Option[XOnlyPublicKey] = None
    decodeTLV(data).foreach {
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

  def decodeNaddr(data: Array[Byte]): Either[Throwable, AddressPointer] =
    var d: String = null
    var relays: List[String] = List.empty
    var author: XOnlyPublicKey = null
    var kind: Int = -1
    decodeTLV(data).foreach {
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
    else Right(AddressPointer(d, kind, author, relays))
}

object NIP19Encoder {
  import NIP19TLV.*

  def encodeNsec(sk: PrivateKey): (String, ByteVector) =
    ("nsec", sk.value.bytes)
  def encodeNpub(pk: XOnlyPublicKey): (String, ByteVector) =
    ("npub", pk.value.bytes)
  def encodeNote(id: ByteVector32): (String, ByteVector) =
    ("note", id.bytes)
  def encodeNprofile(pp: ProfilePointer): (String, ByteVector) =
    (
      "nprofile",
      encodeTLVRecords(
        TLVRecord(TLVType.Special, pp.pubkey.value.bytes) ::
          pp.relays.map(url =>
            TLVRecord(TLVType.Relays, ByteVector.encodeUtf8(url).toTry.get)
          )
      )
    )
  def encodeNevent(evp: EventPointer): (String, ByteVector) = (
    "nevent",
    encodeTLVRecords(
      (TLVRecord(TLVType.Special, ByteVector.fromValidHex(evp.id)) ::
        evp.author.toList.map(author =>
          TLVRecord(TLVType.Author, author.value.bytes)
        )) ++
        evp.relays.map(url =>
          TLVRecord(TLVType.Relays, ByteVector.encodeUtf8(url).toTry.get)
        )
    )
  )
  def encodeNaddr(addr: AddressPointer): (String, ByteVector) =
    (
      "naddr",
      encodeTLVRecords(
        TLVRecord(TLVType.Special, ByteVector.encodeUtf8(addr.d).toTry.get) ::
          TLVRecord(TLVType.Author, addr.author.value.bytes) ::
          TLVRecord(
            TLVType.Kind,
            ByteVector.fromInt(
              addr.kind,
              size = 4,
              ordering = ByteOrdering.BigEndian
            )
          ) ::
          addr.relays.map(url =>
            TLVRecord(TLVType.Relays, ByteVector.encodeUtf8(url).toTry.get)
          )
      )
    )
}

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

  def toByte(t: TLVType): Byte = t match {
    case TLVType.Special => 0
    case TLVType.Relays  => 1
    case TLVType.Author  => 2
    case TLVType.Kind    => 3
    case TLVType.Unknown => throw new Error("can't encode unknown TLV type")
  }
}

object NIP19TLV {
  case class TLVRecord(t: TLVType, v: ByteVector)

  def decodeTLV(data: Array[Byte]): List[TLVRecord] =
    decodeTLVRecords(List.empty, ByteVector(data))

  def decodeTLVRecords(
      records: List[TLVRecord],
      bytes: ByteVector
  ): List[TLVRecord] = {
    if (bytes.size == 0) records
    else
      decodeTLVRecord(bytes)
        .map((rest, record) => decodeTLVRecords(record :: records, rest))
        .getOrElse(List.empty)
  }

  def decodeTLVRecord(
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

  def encodeTLVRecords(records: List[TLVRecord]): ByteVector =
    records.foldLeft(ByteVector.empty)((res, tlv) =>
      res ++ encodeTLVRecord(tlv)
    )

  def encodeTLVRecord(record: TLVRecord): ByteVector =
    ByteVector(TLVType.toByte(record.t), record.v.size.toByte) ++ record.v
}
