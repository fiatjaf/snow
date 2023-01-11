package snow

import io.circe._
import io.circe.syntax._

object Filter {
  given Decoder[Filter] = new Decoder[Filter] {
    final def apply(c: HCursor): Decoder.Result[Filter] = {
      // tag fields
      val tags =
        c.keys
          .map(_.filter(_.startsWith("#")).flatMap { key =>
            c.downField(key).as[List[String]] match {
              case Right(v) => Some((key.drop(1), v))
              case Left(_)  => None
            }
          }.toMap)
          .getOrElse(Map.empty)

      Right(
        Filter(
          ids = c.downField("ids").as[List[String]].toOption,
          authors = c.downField("authors").as[List[String]].toOption,
          kinds = c.downField("kinds").as[List[Int]].toOption,
          since = c.downField("since").as[Long].toOption,
          until = c.downField("until").as[Long].toOption,
          limit = c.downField("limit").as[Int].toOption,
          tags = tags
        )
      )
    }
  }
  given Encoder[Filter] = new Encoder[Filter] {
    final def apply(f: Filter): Json = {
      var fj = JsonObject(
        "ids" := f.ids,
        "authors" := f.authors,
        "kinds" := f.kinds,
        "since" := f.since,
        "until" := f.until,
        "limit" := f.limit
      )

      f.tags.foreachEntry { (k, v) =>
        fj = fj.add(s"#${k}", v.asJson)
      }

      fj.asJson
    }
  }
}

case class Filter(
    ids: Option[List[String]],
    authors: Option[List[String]],
    kinds: Option[List[Int]],
    tags: Map[String, List[String]],
    since: Option[Long],
    until: Option[Long],
    limit: Option[Int]
) {
  def matches(event: Event): Boolean = true
}
