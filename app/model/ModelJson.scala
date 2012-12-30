package model

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Writes._

object ModelJson {
  def itemEvent[T <: ItemEvent](e: String)(fmt: OWrites[T]): Writes[T] = new Writes[T]{
    override def writes(o: T): JsObject = fmt.writes(o) + ("event" -> JsString(e))
  }

  implicit val trackReads = Json.reads[Track]
  implicit val trackWrites = Json.writes[Track]

  implicit val userWrites = new Writes[User] {
    override def writes(o: User) = Json.writes[User].writes(o) + ("avatar" -> JsString(o.avatar))
  }

  //implicit val QueueItemReads = Json.reads[QueueItem]
  implicit val VotesWrites = Json.writes[Votes]
  implicit val QueueItemWrites = Json.writes[QueueItem]

  implicit val TrackAddedWrites = itemEvent("added")(Json.writes[ItemAdded])
  implicit val ItemUpdatedWrites = itemEvent("updated")(Json.writes[ItemUpdated])
  implicit val ItemMovedWrites = itemEvent("moved")(Json.writes[ItemMoved])
  implicit val ItemSkippedWrites = itemEvent("skipped")(Json.writes[ItemSkipped])

  implicit val PlaybackStartedWrites = itemEvent("started")(Json.writes[PlaybackStarted])
  implicit val PlaybackPausedWrites = itemEvent("paused")(Json.writes[PlaybackPaused])
  implicit val PlaybackFinishedWrites = itemEvent("finished")(Json.writes[PlaybackFinished])

  implicit val RoomWrites = new Writes[Room] {
    override def writes(o: Room) = Json.obj(
      "playing" -> o.playing.map(x => Json.toJson(x)),
      "queue" -> JsArray(o.queue.values.toSeq.map(i => Json.toJson(i)))
    )
  }

}