package snow

import scala.util.chaining.*
import io.circe.*
import io.circe.syntax.*

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
          authors =
            c.downField("authors").as[List[String]].getOrElse(List.empty),
          kinds = c.downField("kinds").as[List[Int]].getOrElse(List.empty),
          ids = c.downField("ids").as[List[String]].getOrElse(List.empty),
          tags = tags,
          since = c.downField("since").as[Long].toOption,
          until = c.downField("until").as[Long].toOption,
          limit = c.downField("limit").as[Int].toOption
        )
      )
    }
  }
  given Encoder[Filter] = new Encoder[Filter] {
    final def apply(f: Filter): Json = {
      var fj = JsonObject()
      if (f.ids.size > 0) fj = fj.add("ids", f.ids.asJson)
      if (f.authors.size > 0) fj = fj.add("authors", f.authors.asJson)
      if (f.kinds.size > 0) fj = fj.add("kinds", f.kinds.asJson)
      f.since.foreach { v => fj = fj.add("since", v.asJson) }
      f.until.foreach { v => fj = fj.add("until", v.asJson) }
      f.limit.foreach { v => fj = fj.add("limit", v.asJson) }
      f.tags.foreachEntry { (k, v) =>
        fj = fj.add(s"#${k}", v.asJson)
      }
      fj.asJson
    }
  }
}

case class Filter(
    ids: List[String] = List.empty,
    authors: List[String] = List.empty,
    kinds: List[Int] = List.empty,
    tags: Map[String, List[String]] = Map.empty,
    since: Option[Long] = None,
    until: Option[Long] = None,
    limit: Option[Int] = None
) {
  def matches(event: Event): Boolean =
    (ids.isEmpty || ids.contains(event.id)) &&
      (kinds.isEmpty || kinds.contains(event.kind)) &&
      (authors.isEmpty || authors.contains(event.pubkey)) &&
      tags
        .map { case ((tag, vals)) =>
          event
            .getTagValues(tag)
            .exists(tagValue => vals.contains(tagValue))
        }
        .forall(_ == true) &&
      since.map(event.created_at > _).getOrElse(true) &&
      until.map(event.created_at < _).getOrElse(true)
}
