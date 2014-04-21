package controllers

import lib.RdioApi
import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

object Rdio extends Controller {

  def search(term: String) = Action.async {
    RdioApi.call("search",  Map(
      "types" -> Seq("Artist", "Album", "Track"),
      "query" -> Seq(term),
      "extras" -> Seq("albumCount")
    )).map(i => Ok(i.json))
  }

  def artist(id: String) = Action.async {
    RdioApi.call("get",  Map(
      "keys" -> Seq(id),
      "extras" -> Seq("albums")
    )).flatMap(artistInfoResponse =>
      RdioApi.call("getAlbumsForArtist", Map(
        "artist" -> Seq(id)
      )).map(albumResponse =>
        Ok(Json.toJson(Map(
            "artistInfo" -> (artistInfoResponse.json \ "result" \ id),
            "albums" -> (albumResponse.json \ "result")
          ))
        )
      )
    )
  }

  def album(id: String) = Action.async {
    RdioApi.call("get",  Map(
      "keys" -> Seq(id),
      "extras" -> Seq("tracks")
    )).map(i => Ok(i.json \ "result" \ id))
  }
}
