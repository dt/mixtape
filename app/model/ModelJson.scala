package model

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Writes._

object ModelJson {
  def event[T <: Event](e: String)(fmt: OWrites[T]): Writes[T] = new Writes[T]{
    override def writes(o: T): JsObject = fmt.writes(o) + ("event" -> JsString(e))
  }

  implicit val ErrorMsgWrites = event("error")(Json.writes[ErrorMsg])

  implicit val trackReads = Json.reads[Track]
  implicit val trackWrites = Json.writes[Track]

  implicit val userWrites = new Writes[User] {
    override def writes(o: User) = Json.writes[User].writes(o) + ("avatar" -> JsString(o.avatar))
  }
  implicit val userReads = Json.reads[User]

  implicit val votesWrites = Json.writes[Votes]
  implicit val votesReads = Json.reads[Votes]
  implicit val queueItemWrites = Json.writes[QueueItem]
  implicit val queueItemReads = Json.reads[QueueItem]
  implicit val listReads = Reads.traversableReads[Seq, QueueItem]
  implicit val listWrites = Writes.traversableWrites[QueueItem]
  implicit val chatWrites = event("chat")(Json.writes[Chat])
  implicit val leaveJoinWrites = event("leavejoin")(Json.writes[LeaveJoin])

  implicit val queueReads = Json.reads[Queue]
  implicit val queueWrites = Json.writes[Queue]

  implicit val trackAddedWrites = event("added")(Json.writes[ItemAdded])
  implicit val itemUpdatedWrites = new Writes[ItemUpdated] {
    override def writes(o: ItemUpdated) =  Json.obj(
      "event" -> JsString("updated"),
      "item" -> (queueItemWrites.writes(o.item) + ("skipping" -> JsBoolean(o.item.shouldSkip)))
    )
  }
  implicit val itemMovedWrites = event("moved")(Json.writes[ItemMoved])
  implicit val itemSkippedWrites = event("skipped")(Json.writes[ItemSkipped])
  implicit val playbackSkippedWrites = event("playing-song-skipped")(Json.writes[PlaybackSkipped])

  implicit val playbackStartedWrites = event("started")(Json.writes[PlaybackStarted])
  implicit val playbackFinishedWrites = new Writes[PlaybackFinished.type] {
    override def writes(o: PlaybackFinished.type) = Json.obj("event" -> JsString("finished"))
  }
  implicit val playbackProgressWrites = event("progress")(Json.writes[PlaybackProgress])

  implicit val startBroadcastingWrites = new Writes[StartBroadcasting.type] {
    override def writes(o: StartBroadcasting.type) = Json.obj("event" -> JsString("start-broadcasting"))
  }
  implicit val stopBroadcastingWrites = new Writes[StopBroadcasting.type] {
    override def writes(o: StopBroadcasting.type) = Json.obj("event" -> JsString("stop-broadcasting"))
  }


  implicit val roomWrites = new Writes[Room] {
    override def writes(o: Room) = Json.obj(
      "playing" -> o.playing.map(x => queueItemWrites.writes(x) + ("position" -> JsNumber(o.playbackPosition))),
      "lurkers" -> JsNumber(o.anonUsers),
      "listeners" -> JsArray(o.listening.toSeq.flatMap(i => o.users.get(i).map(u => Json.toJson(u.user)))),
      "broadcasters" -> JsArray(o.broadcasting.toSeq.flatMap(i => o.users.get(i).map(u => Json.toJson(u.user)))),
      "queue" -> Json.toJson(o.queue.values.toSeq)
    )
  }

}