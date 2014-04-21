package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import lib.{Gravatar, RdioApi}
import model._
import model.ModelJson._

object Application extends Controller with Secured {
  val admin = User("admin", "music@foursquare.com", "", "")
  Room("ny", Some(admin))
  Room("sf", Some(admin))

  def index(roomId: String) = MaybeAuthenticated { implicit request =>
    Room(roomId.toLowerCase, request.userOpt).map { room =>
      val domain = request.domain
      RdioApi.call("getPlaybackToken", Map("domain" -> Seq(domain))).map{ token =>
        Ok(views.html.index(room.name, Room.list, (token.json \ "result").as[String], request.userOpt.isDefined))
      }
    } getOrElse {
      Future(NotFound("This room doesn't exist yet. You need to be logged in to create rooms."))
    }
  }

  def queue(roomId: String) = Action {
    Room(roomId.toLowerCase, None).map(r => Ok(Json.toJson(r))).getOrElse(NotFound)
  }

  def controls(roomId: String) = WebSocket.using[JsValue] { implicit request =>
    Room(roomId.toLowerCase, user(request)).map { room =>
      val iteratee = Iteratee.foreach[JsValue] { case o: JsObject =>
        user(request).foreach { u =>
          if ((o \ "event").asOpt[String].forall(_ != "progress"))
            Logger.debug("websocket message: " + o.toString)
          try {
            (o \ "event").asOpt[String] match {
              case Some("chat") =>
                room.say((o \ "msg").as[String], u)
              case Some("add") =>
                Json.fromJson[Track](o).foreach(room.enqueue(_, u))
              case Some("voteup") =>
                room.voteUp((o \ "id").as[String], u)
              case Some("votedown") =>
                room.voteDown((o \ "id").as[String], u)
              case Some("move") =>
                room.moveItem((o \ "id").as[String], (o \ "putBefore").asOpt[String].filterNot(_ == ""), u)
              case Some("finished") =>
                room.finishedPlaying((o \ "id").as[String], u)
              case Some("progress") =>
                room.updatePlaybackPosition((o \ "pos").as[Double], (o \ "ts").as[Long], u)
              case Some("listening") =>
                room.startedListening(u)
              case Some("stopped-listening") =>
                room.stoppedListening(u)
              case Some("broadcasting") =>
                room.startedBroadcasting(u)
              case Some("stopped-broadcasting") =>
                room.stoppedBroadcasting(u)

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
      }.map { _ =>
        room.left(user(request))
      }

      (iteratee, room.join(user(request)))
    } getOrElse {
      Done[JsValue,Unit]((),Input.EOF) ->
        Enumerator[JsValue](JsObject(Seq("error" -> JsString("no such room")))).andThen(Enumerator.enumInput(Input.EOF))
    }
  }

  def debug = Action { implicit request =>
    Ok(request.host)
  }
}

