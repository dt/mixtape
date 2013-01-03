package model

import scala.collection.mutable
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
  var playbackPosition = 0.0
  val queue = new lib.HashQueue[String, QueueItem]
  val (bcast, channel) = Concurrent.broadcast[JsValue]

  val channels = mutable.Map.empty[String, PushEnumerator[JsValue]]
  val users = mutable.Map.empty[String, User]
  val listening = mutable.Set.empty[String]
  val broadcasting = mutable.Set.empty[String]

  var anonUsers = 0

  def join(user: Option[User]) = {
    user match {
      case Some(u) => {
        val unicast = Enumerator.imperative[JsValue](onStart = () => this.joined(u))
        channels.put(u.id, unicast)
        unicast.interleave(bcast)
      }
      case None => {
        anonUsers += 1
        bcast
      }
    }
  }

  def joined(u: User) = {
    users.put(u.id, u)
  }

  def left(user: Option[User]) = {
    user match {
      case Some(u) => {
        users.remove(u.id)
        stoppedListening(u)
        channels.remove(u.id)
        checkBroadcasting()
      }
      case None => anonUsers -= 1
    }
  }

  def enqueue(track: Track, by: User) = {
    val e = QueueItem(track.id, track , by)
    queue.push(track.id -> e)
    channel.push(Json.toJson(ItemAdded(e)))
    if (queue.size == 1 && playing.isEmpty)
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

  def shouldSkip(item: QueueItem): Boolean =
    item.votes.down.size > item.votes.up.size

  def playNext(who: User) {
    queue.pop() match {
      case None => {
        playing = None
        channel.push(Json.toJson(PlaybackFinished))
      }
      case Some(next) if shouldSkip(next) => {
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
      channels.get(u.id).foreach(_.push(Json.toJson(StartBroadcasting)))
  }

  def stoppedListening(u: User) = {
    stoppedBroadcasting(u)
    listening.remove(u.id)
  }

  def startedBroadcasting(u: User) = {
    if (broadcasting.size > 0)
      broadcasting.flatMap(channels.get).foreach(_.push(Json.toJson(StopBroadcasting)))
    broadcasting.add(u.id)
  }

  def stoppedBroadcasting(u: User) = {
    broadcasting.remove(u.id)
    checkBroadcasting(not = Some(u))
  }

  def checkBroadcasting(not: Option[User] = None) = {
    if (broadcasting.size < 1)
      not.map(u => listening.filterNot(_ == u.id)).filterNot(_.isEmpty).getOrElse(listening)
        .collectFirst(channels).foreach(_.push(Json.toJson(StartBroadcasting)))
  }
}