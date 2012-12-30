package controllers

import lib.Gravatar
import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import model._

import model.ModelJson._

object Application extends Controller with Secured {
  Room("ny")
  Room("sf")

  def index(roomId: String) = MaybeAuthenticated { implicit request =>
    val room = Room(roomId.toLowerCase)
    Ok(views.html.index(room.name, Room.list))
  }

  def queue(roomId: String) = Action {
    val room = Room(roomId.toLowerCase)
    Ok(Json.toJson(room))
  }

  def controls(roomId: String) = WebSocket.using[JsValue] { implicit request =>
    val room = Room(roomId.toLowerCase)

    val iteratee = Iteratee.foreach[JsValue] { case o: JsObject =>
      user(request).foreach{ u =>
        Logger.debug("websocket message: " + o.toString)
        (o \ "event").asOpt[String] match {
          case Some("add") =>
            Json.fromJson[Track](o).foreach(room.add(_, u))
          case Some("voteup") =>
            (o \ "id").asOpt[String].foreach(room.voteUp(_, u))
          case Some("votedown") =>
            (o \ "id").asOpt[String].foreach(room.voteDown(_, u))
          case Some("move") => for {
            id <- (o \ "id").asOpt[String]
            putBefore = (o \ "putBefore").asOpt[String].filterNot(_ == "")
          } room.move(id, putBefore, u)
          case Some("finished") => room.playNext()
          case Some(unknown) => Logger.error("unknown event: " + unknown)
          case None => Logger.error("missing event! " + o.toString)
        }
      }; case _ => //pass
    }

    (iteratee, room.enum)
  }

  def debug = Action { implicit request =>
    Ok(request.host)
  }
}

