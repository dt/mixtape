package controllers

import lib.RdioApi
import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Rdio extends Controller {

  def search(term: String) = Action { Async {
    RdioApi.call("search",  Map(
      "types" -> Seq("Artist", "Album", "Track"),
      "query" -> Seq(term),
      "extras" -> Seq("albumCount")
    )).map(i => Ok(i.json))
  }}

  def artist(id: String) = Action { Async {
    RdioApi.call("get",  Map(
      "keys" -> Seq(id)
    )).map(i => Ok(i.json \ "result" \ id))
  }}

  def album(id: String) = Action { Async {
    RdioApi.call("get",  Map(
      "keys" -> Seq(id),
      "extras" -> Seq("tracks")
    )).map(i => Ok(i.json \ "result" \ id))
  }}
}