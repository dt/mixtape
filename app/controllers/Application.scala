package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import lib.{Gravatar, RdioApi}
import model._
import model.ModelJson._

object Application extends Controller with Secured {
  Room("ny")
  Room("sf")

  def index(roomId: String) = MaybeAuthenticated { implicit request =>
    val room = Room(roomId.toLowerCase)
    val domain = request.domain
    Async {
      RdioApi.call("getPlaybackToken", Map("domain" -> Seq(domain))).map{ token =>
        Ok(views.html.index(room.name, Room.list, (token.json \ "result").as[String]))
      }
    }
  }

  def queue(roomId: String) = Action {
    val room = Room(roomId.toLowerCase)
    Ok(Json.toJson(room))
  }

  def controls(roomId: String) = WebSocket.using[JsValue] { implicit request =>
    val room = Room(roomId.toLowerCase)

    val iteratee = Iteratee.foreach[JsValue] { case o: JsObject =>
      user(request).foreach { u =>
        Logger.debug("websocket message: " + o.toString)
        try {
          (o \ "event").asOpt[String] match {
            case Some("add") =>
              Json.fromJson[Track](o).foreach(room.enqueue(_, u))
            case Some("voteup") =>
              (o \ "id").asOpt[String].foreach(room.voteUp(_, u))
            case Some("votedown") =>
              (o \ "id").asOpt[String].foreach(room.voteDown(_, u))
            case Some("move") => for {
              id <- (o \ "id").asOpt[String]
              putBefore = (o \ "putBefore").asOpt[String].filterNot(_ == "")
            } room.moveItem(id, putBefore, u)
            case Some("finished") => room.finishedPlaying((o \ "id").as[String], u)
            case Some("progress") => room.updatePlaybackPosition((o \ "pos").as[Double], (o \ "ts").as[Long], u)

            case Some("listening") => room.startedListening(u)
            case Some("stopped-listening") => room.stoppedListening(u)
            case Some("broadcasting") => room.startedBroadcasting(u)
            case Some("stopped-broadcasting") => room.stoppedBroadcasting(u)

            case Some(unknown) => {
              val msg = "unknown event: " + unknown
              room.sendError(u.id, msg)
              Logger.error(msg)
            }
            case None => Logger.error("missing event! " + o.toString)
          }
        } catch {
          case e: Exception => {
            Logger.error("Error handling event!", e)
            room.sendError(u.id, e.toString)
          }
        }
      }; case _ => //pass
    }.mapDone { _ =>
      room.left(user(request))
    }

    (iteratee, room.join(user(request)))
  }

  def debug = Action { implicit request =>
    Ok(request.host)
  }
}

