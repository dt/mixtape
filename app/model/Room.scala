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

  load()

  val (bcast, everyone) = Concurrent.broadcast[JsValue]

  val users = mutable.Map.empty[String, UserChannel]
  val listening = mutable.Set.empty[String]
  val broadcasting = mutable.Set.empty[String]

  var anonUsers = 0

  def join(user: Option[User]) = {
    user match {
      case Some(u) => {
        val unicast = Concurrent.unicast[JsValue](onStart = channel => {
          guestlistChanged()
          users.put(u.id, UserChannel(u, channel))
        })

        unicast.interleave(bcast)
      }
      case None => {
        anonUsers += 1
        guestlistChanged()
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

  def guestlistChanged() = {
    val guestlist = LeaveJoin(
      users.values.map(_.user).toSet,
      listening.flatMap(users.get(_).map(_.user)).toSet,
      anonUsers)

    everyone.push(Json.toJson(guestlist))
  }

  def say(msg: String, who: User) = {
    everyone.push(Json.toJson(Chat(msg, who)))
  }

  def enqueue(track: Track, by: User) = {
    val id = java.util.UUID.randomUUID().toString()
    val e = QueueItem(id, track , by)

    queue.push(e.id -> e)
    everyone.push(Json.toJson(ItemAdded(e)))
    if (playing.isEmpty && queue.size == 1)
      playNext()
    dump()
  }

  def updated(item: QueueItem) = {
    everyone.push(Json.toJson(ItemUpdated(item)))
    dump()
  }

  def voteUp(id: String, who: User) = {
    playing.filter(_.id == id).orElse(queue.get(id)).filterNot(_.by == who).foreach { item =>
      item.votes.up += who
      updated(item)
    }
  }

  def voteDown(id: String, who: User) = {
    // check the currently playing track first
    playing.filter(_.id == id) match {
      case Some(item) if item.by == who => // enqueuer wants to skip currently playing track
        skipPlaying(item)
      case Some(item) => { // someone else wants to skip currently playing track
        item.votes.down += who
        if (item.votes.down.size > item.votes.up.size) skipPlaying(item) else updated(item)
      }
      case None => queue.get(id).foreach { item => // look for the downvoted item in the queue
        if (item.by == who) { // enqueuer's downvote results in instant removal
          queue.remove(id)
          dump()
          everyone.push(Json.toJson(ItemSkipped(item.id)))
        } else {
          item.votes.down += who
          updated(item)
        }
      }
    }
  }

  def skipPlaying(item: QueueItem) = {
    playing = None
    playNext()
    everyone.push(Json.toJson(PlaybackSkipped(item.id)))
    dump()
  }

  def moveItem(id: String, nowBefore: Option[String], who: User) = {
    val item = queue(id)
    nowBefore match {
      case Some(before) => queue.moveTo(id, before)
      case None => queue.remove(id).foreach(x => queue.push(id -> x))
    }
    everyone.push(Json.toJson(ItemMoved(id, nowBefore.getOrElse(""))))
    dump()
  }

  def finishedPlaying(id: String, who: User) = {
    if (playing.exists(_.id == id))
      playNext()
  }

  protected def playNext() {
    queue.pop() match {
      case None => {
        playing = None
        everyone.push(Json.toJson(PlaybackFinished))
      }
      case Some(next) if next.shouldSkip => {
        everyone.push(Json.toJson(ItemSkipped(next.id)))
        playNext()
      }
      case Some(next) =>
        playing = Some(next)
        playbackPosition = 0.0
        everyone.push(Json.toJson(PlaybackStarted(next)))
    }
    dump()
  }

  def updatePlaybackPosition(pos: Double, ts: Long, who: User) = {
    playbackPosition = pos
    everyone.push(Json.toJson(PlaybackProgress(pos, ts)))
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

  def roomFile = new java.io.File("rooms/", name.replaceAll("[^a-z]", "") + ".json")

  def load() = {
    try {
      val dump = Json.parse(play.api.libs.Files.readFile(roomFile)).as[Queue]
      playing = dump.playing
      dump.queue.foreach(i => queue.push(i.id -> i))
    } catch {
      case e: Exception => //Logger.warn("could not load queue", e)
    }
  }

  def dump() = {
    try {
      play.api.libs.Files.writeFile(roomFile, Json.toJson(Queue(playing, queue.values.toList)).toString)
    } catch {
      case e: Exception => //Logger.warn("could not write queue", e)
    }
  }
}