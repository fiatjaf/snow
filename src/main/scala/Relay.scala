package snow

import scala.concurrent.duration.*
import scoin.{Crypto, ByteVector32}
import scodec.bits.ByteVector
import fs2.{io => *, *}
import fs2.concurrent.{Topic, Channel}
import cats.implicits.*
import cats.effect.*
import org.http4s.Uri
import org.http4s.client.websocket.*
import org.http4s.dom.*
import io.circe.*
import io.circe.parser.decode
import io.circe.syntax.*
import cats.Show

/**
  * An implementation of Relay[F] provides methods like `subscribe` 
  * and access to the underlying streams of things while
  * hiding the details about how it gets the things.
  */
trait Relay[F[_]]:
  def uri: Uri
  def nextId: Ref[F, Int]
  def commands: Channel[F, Json]
  def events: Topic[F, (String, Event)]
  /**
   * subscribing with a `Filter` will give us a list of stored events
   * and a stream of future events */
  def subscribe( filter: Filter*): Resource[F, (List[Event], Stream[F,Event])]

object Relay {

  /* this should ideally be for a generic F[_], but we hard code it to F = IO */
  def apply(uri: Uri): Resource[IO, Relay[IO]] = mkResourceForIO(uri)

  /* can make this more generic eventually, but for now it is tied to F = IO */
  def mkResourceForIO(uri: Uri, debugOn: Boolean = false): Resource[IO, Relay[IO]] =

    def debug[A](x: A)(using Show[A]) = if(debugOn) then IO.println(x) else IO.unit

    for
      nextId <- Ref[IO].of(0).toResource
      commands <- Channel.unbounded[IO, Json].toResource
      events <- Topic[IO, (String, Event)].toResource
      conn <- WebSocketClient[IO].connectHighLevel(WSRequest(uri))

      // here we weave together the websocket streams and start the background
      // process that keeps them going
      background <- {
        val receive = debug("opening receive stream") *> conn.receiveStream
          .collect { case WSFrame.Text(line, _) => line }
          .map(line => decode[List[Json]](line.toString))
          .collect { case Right(v) => v }
          .evalTap[IO, Unit] { msg =>
            msg match {
              case msg if msg.size == 2 && msg(0).as[String] == Right("EOSE") =>
                msg(1).as[String] match {
                  case Right(subid) => events.publish1((subid, Event(kind = -1, content = ""))).void 
                                          *> debug(s"$subid: eose")
                  case _            => IO.unit
                }
              case msg if msg.size == 3 && msg(0).as[String] == Right("EVENT") =>
                (msg(1).as[String], msg(2).as[Event]) match {
                  case (Right(subid), Right(event)) if event.isValid =>
                    debug(s"$subid: ${event.hash}")
                     *> events.publish1((subid, event)).void
                  case _ => IO.unit
                }
              case _ => IO.unit
            }
          }
          .compile
          .drain

        val send = debug("opening send stream") *> commands.stream
          .evalMap { msg => debug(s"sending request: $msg") *> conn.sendText(msg.noSpaces) }
          .compile
          .drain

        (send, receive).parTupled.void.background
      }
      // only thing left to do now is return our Relay
    yield new RelayImplForIO(uri, nextId, commands, events, debugOn)
}

class RelayImplForIO(
    val uri: Uri,
    val nextId: Ref[IO, Int],
    val commands: Channel[IO, Json],
    val events: Topic[IO, (String, Event)],
    debugOn: Boolean
) extends Relay[IO]{

  def debug[A](x: A)(using Show[A]) = if(debugOn) then IO.println(x.show) else IO.unit

  def subscribe(
      filter: Filter*
  ): Resource[IO, (List[Event], fs2.Stream[IO, Event])] = {
    nextId.getAndUpdate(_ + 1).map(_.toString).toResource.flatMap { 
      currId =>
        val send = commands.send(
          Seq("REQ".asJson, currId.asJson)
            .concat(filter.map(_.asJson))
            .asJson
        )

        val receive =
          events
            .subscribe(1)
            .collect {
              case (subid, event) if subid == currId => event
            }

        // we make sure to trigger `send` first
        send.background *> splitHistorical(receive)
    }
  }

  /** split into historical versus live events, where an event of kind -1 designates
   *  the marker between past and present
    from SystemFw's help here: https://discord.com/channels/632277896739946517/632310980449402880/1337198474252255264
    */
  def splitHistorical(in: Stream[IO, Event]): Resource[IO, (List[Event], Stream[IO, Event])] =
    (Deferred[IO, List[Event]].toResource, Channel.unbounded[IO, Event].toResource)
      .flatMapN { (historical, live) =>
        def split(in: Stream[IO, Event], acc: Chunk[Event] = Chunk.empty): Pull[IO, Event, Unit] =
          in
            .pull
            .uncons1 // ideally done with uncons + chunk split, left for the reader
            .flatMap {
              case None => Pull.done
              case Some((n, rest)) =>
                if n.kind == - 1 
                then Pull.eval(historical.complete(acc.toList)) >> rest.pull.echo
                else split(rest, acc ++ Chunk(n))
            }

        split(in)
          .stream
          .through(live.sendAll)
          .compile
          .drain
          .background
          .evalMap{ _ =>
            historical.get.tupleRight(live.stream)
          }
      }
}
