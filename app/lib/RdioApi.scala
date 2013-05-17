package lib

import play.api.Play
import play.api.Play.current
import play.api.libs.concurrent.Promise
import play.api.libs.json._
import play.api.libs.oauth._
import play.api.libs.ws.WS
import play.api.Logger
import scala.concurrent.{Future, Promise}
import play.api.mvc.RequestHeader
import scala.concurrent.ExecutionContext.Implicits.global

object RdioApi {

  def cfg(k: String) = Play.application.configuration.getString(k).getOrElse(throw new NoSuchElementException(k))

  val clientToken: Future[String] = WS.url("https://www.rdio.com/oauth2/token")
    .post(Map(
      "grant_type"    -> Seq("client_credentials"),
      "client_id"     -> Seq(cfg("rdio.key")),
      "client_secret" -> Seq(cfg("rdio.secret"))
      )
    ).map { token =>
      Logger.info("Rdio Client Access Token: " + token.body)
      (token.json \ "access_token").as[String]
    }

  def call(method: String, params: Map[String, Seq[String]] = Map.empty) = {
    clientToken.flatMap(token =>
      WS.url("https://www.rdio.com/api/1/")
        .post(Map("method" -> Seq(method), "access_token" -> Seq(token)) ++ params)
    )
  }
}