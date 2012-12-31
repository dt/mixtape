package model

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Writes._

object ModelJson {
  def event[T <: Event](e: String)(fmt: OWrites[T]): Writes[T] = new Writes[T]{
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

  implicit val TrackAddedWrites = event("added")(Json.writes[ItemAdded])
  implicit val ItemUpdatedWrites = event("updated")(Json.writes[ItemUpdated])
  implicit val ItemMovedWrites = event("moved")(Json.writes[ItemMoved])
  implicit val ItemSkippedWrites = event("skipped")(Json.writes[ItemSkipped])

  implicit val PlaybackStartedWrites = event("started")(Json.writes[PlaybackStarted])
  implicit val PlaybackFinishedWrites = new Writes[PlaybackFinished.type] {
    override def writes(o: PlaybackFinished.type) = Json.obj("event" -> JsString("finished"))
  }
  implicit val PlaybackProgressWrites = event("progress")(Json.writes[PlaybackProgress])

  implicit val RoomWrites = new Writes[Room] {
    override def writes(o: Room) = Json.obj(
      "playing" -> o.playing.map(x => QueueItemWrites.writes(x) + ("position" -> JsNumber(o.playbackPosition))),
      "queue" -> JsArray(o.queue.values.toSeq.map(i => Json.toJson(i)))
    )
  }

}