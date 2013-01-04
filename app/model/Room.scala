package model

import scala.collection.mutable
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Concurrent._
import play.api.libs.json._
import play.api.libs.json.Json._
import ModelJson._

object Room {
  private val all = scala.collection.mutable.HashMap.empty[String, Room]
  def apply(name: String) = this.synchronized { all.getOrElseUpdate(name, new Room(name)) }
  def list = all.keys
}

case class UserChannel(user: User, channel: Channel[JsValue])

class Room(val name: String) {
  var playing: Option[QueueItem] = None
  var playbackPosition = 0.0
  val queue = new lib.HashQueue[String, QueueItem]
  val (bcast, channel) = Concurrent.broadcast[JsValue]

  val users = mutable.Map.empty[String, UserChannel]
  val listening = mutable.Set.empty[String]
  val broadcasting = mutable.Set.empty[String]

  var anonUsers = 0

  def join(user: Option[User]) = {
    user match {
      case Some(u) => {
        val unicast = Concurrent.unicast[JsValue](onStart = channel => users.put(u.id, UserChannel(u, channel)))
        unicast.interleave(bcast)
      }
      case None => {
        anonUsers += 1
        bcast
      }
    }
  }

  def left(user: Option[User]) = {
    user match {
      case Some(u) => {
        users.remove(u.id)
        stoppedListening(u)
        users.remove(u.id)
        checkBroadcasting()
      }
      case None => anonUsers -= 1
    }
  }

  def enqueue(track: Track, by: User) = {
    val e = QueueItem(track.id, track , by)
    queue.push(track.id -> e)
    channel.push(Json.toJson(ItemAdded(e)))
    if (playing.isEmpty && queue.size == 1)
      playNext(by)
  }

  def voteUp(id: String, who: User) = {
    val item = queue(id)
    item.votes.up += who
    channel.push(Json.toJson(ItemUpdated(item)))
  }

  def voteDown(id: String, who: User) = {
    (queue.get(id), playing) match {
      case (_, Some(item)) if item.id == id => {
        item.votes.down += who
        if (item.by == who || item.votes.down.size > (item.votes.up + item.by).size) {
          playing = None
          channel.push(Json.toJson(PlaybackSkipped(item.id)))
          playNext(who)
        } else {
          channel.push(Json.toJson(ItemUpdated(item)))
        }
      }
      case (Some(item), _) => {
        if (item.by == who) {
          queue.remove(id)
          channel.push(Json.toJson(ItemSkipped(item.id)))
        } else {
          item.votes.down += who
          channel.push(Json.toJson(ItemUpdated(item)))
        }
      }
    }
  }

  def moveItem(id: String, nowBefore: Option[String], who: User) = {
    val item = queue(id)
    nowBefore match {
      case Some(before) => queue.moveTo(id, before)
      case None => queue.remove(id).foreach(x => queue.push(id -> x))
    }
    channel.push(Json.toJson(ItemMoved(id, nowBefore.getOrElse(""))))
  }

  def finishedPlaying(id: String, who: User) = {
    if (playing.exists(_.id == id))
      playNext(who)
  }

  protected def playNext(who: User) {
    queue.pop() match {
      case None => {
        playing = None
        channel.push(Json.toJson(PlaybackFinished))
      }
      case Some(next) if next.shouldSkip => {
        channel.push(Json.toJson(ItemSkipped(next.id)))
        playNext(who)
      }
      case Some(next) =>
        playing = Some(next)
        playbackPosition = 0.0
        channel.push(Json.toJson(PlaybackStarted(next)))
    }
  }

  def updatePlaybackPosition(pos: Double, ts: Long, who: User) = {
    playbackPosition = pos
    channel.push(Json.toJson(PlaybackProgress(pos, ts)))
  }

  def startedListening(u: User) = {
    listening.add(u.id)
    if (broadcasting.size < 1)
      users.get(u.id).foreach(_.channel.push(Json.toJson(StartBroadcasting)))
  }

  def stoppedListening(u: User) = {
    listening.remove(u.id)
    stoppedBroadcasting(u)
  }

  def startedBroadcasting(u: User) = {
    if (broadcasting.size > 0)
      broadcasting.flatMap(users.get).foreach(_.channel.push(Json.toJson(StopBroadcasting)))
    broadcasting.add(u.id)
  }

  def stoppedBroadcasting(u: User) = {
    broadcasting.remove(u.id)
    checkBroadcasting(not = Some(u))
  }

  protected def checkBroadcasting(not: Option[User] = None) = {
    if (broadcasting.size < 1)
      not.map(u => listening.filterNot(_ == u.id)).filterNot(_.isEmpty).getOrElse(listening)
        .collectFirst(users).foreach(_.channel.push(Json.toJson(StartBroadcasting)))
  }

  def sendError(to: String, msg: String) = {
    users.get(to).foreach(_.channel.push(Json.toJson(ErrorMsg(msg))))
  }
}