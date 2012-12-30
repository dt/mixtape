package model

import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.Json._
import ModelJson._


object Room {
  private val all = scala.collection.mutable.HashMap.empty[String, Room]
  def apply(name: String) = this.synchronized { all.getOrElseUpdate(name, new Room(name)) }
  def list = all.keys
}

class Room(val name: String) {
  var playing: Option[QueueItem] = None
  val queue = new lib.HashQueue[String, QueueItem]
  val (enum, channel) = Concurrent.broadcast[JsValue]

  def add(track: Track, by: User) = {
    val e = QueueItem(track.id, track , by)
    queue.push(track.id -> e)
    channel.push(Json.toJson(ItemAdded(e)))
  }

  def voteUp(id: String, who: User) = {
    val item = queue(id)
    item.votes.up += who
    channel.push(Json.toJson(ItemUpdated(item)))
  }

  def voteDown(id: String, who: User) = {
    val item = queue(id)
    item.votes.down += who
    channel.push(Json.toJson(ItemUpdated(item)))
  }

  def move(id: String, nowBefore: Option[String], who: User) = {
    val item = queue(id)
    if (item.by == who && nowBefore.forall(queue(_).by == who)) {
      nowBefore match {
        case Some(before) => queue.moveTo(id, before)
        case None => queue.remove(id).foreach(x => queue.push(id -> x))
      }
      channel.push(Json.toJson(ItemMoved(id, nowBefore.getOrElse(""))))
    }
  }

  def shouldSkip(item: QueueItem): Boolean = item.votes.down.size > item.votes.up.size

  def playNext() {
    queue.pop() match {
      case None => playing = None
      case Some(next) if shouldSkip(next) => {
        channel.push(Json.toJson(ItemSkipped(next.id)))
        playNext()
      }
      case Some(next) =>
        playing = Some(next)
        channel.push(Json.toJson(PlaybackStarted(next.id)))
    }
  }
}