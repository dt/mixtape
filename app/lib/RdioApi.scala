package lib

import play.api.Play
import play.api.Play.current
import play.api.libs.concurrent.Promise
import play.api.libs.json._
import play.api.libs.oauth._
import play.api.libs.ws.WS
import play.api.Logger
import play.api.mvc.RequestHeader
import scala.concurrent.ExecutionContext.Implicits.global

object RdioApi {
  def cfg(k: String) = Play.application.configuration.getString(k).getOrElse(throw new NoSuchElementException(k))
  val Keys = ConsumerKey(cfg("rdio.key"), cfg("rdio.secret"))
  val Tokenless = RequestToken("", "")
  val Signer = OAuthCalculator(Keys, Tokenless)

  def call(method: String, params: Map[String, Seq[String]] = Map.empty) = {
    WS.url("http://api.rdio.com/1/")
      .sign(Signer)
      .post(Map("method" -> Seq(method)) ++ params)
  }
}