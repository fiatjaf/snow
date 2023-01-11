package snow

import cats.effect._
import scala.collection.mutable
import org.http4s.Uri
import org.http4s.dom._
import org.http4s.client.websocket._
import io.circe._
import io.circe.parser.decode
import io.circe.syntax._

object Relay {
  val relays = mutable.Map.empty[String, Relay]

  def apply(url: String): Either[Throwable, Relay] =
    Uri.fromString(url).map(_.toOriginForm).map { uri =>
      val url = uri.toString
      relays.get(url).getOrElse {
        val relay = new Relay(uri)
        relays += url -> relay
        relay
      }
    }
}

class Relay(uri: Uri) {
  WebSocketClient[IO].connectHighLevel(WSRequest(uri)).use { conn =>
    var nextId = 0

    def publish(event: Event): IO[Unit] =
      conn.sendText(Seq("EVENT".asJson, event.asJson).asJson.noSpaces)

    val stream =
      conn.receiveStream.map(df => decode[List[Json]](df.toString)).collect {
        case Right(v) => v
      }

    val events =
      stream
        .filter(msg => msg.size == 3 && msg(0).as[String] == Right("EVENT"))
        .map(msg => (msg(1).as[String], msg(2).as[Event]))
        .collect {
          case (Right(subid), Right(event)) if event.isValid => (subid, event)
        }

    def subscribe(filter: Filter*) = {
      nextId += 1
      val id = nextId.toString()
      conn.sendText(
        Seq("REQ".asJson, id.asJson)
          .concat(filter.map(_.asJson))
          .asJson
          .noSpaces
      )
      events.filter { case (subid, event) => subid == id }
    }

    IO.unit
  }
}
