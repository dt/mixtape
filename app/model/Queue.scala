package model

import scala.collection.mutable

case class Votes(up: mutable.Set[User] = mutable.Set.empty, down: mutable.Set[User] = mutable.Set.empty)
case class QueueItem(id: String, track: Track, by: User, votes: Votes = Votes())


trait Event
trait ItemEvent extends Event
trait PlaybackEvent extends Event

case class ItemAdded(item: QueueItem) extends ItemEvent
case class ItemUpdated(item: QueueItem) extends ItemEvent
case class ItemMoved(id: String, nowBefore: String) extends ItemEvent
case class ItemSkipped(id: String) extends ItemEvent

case class PlaybackStarted(item: QueueItem) extends PlaybackEvent
case object PlaybackFinished extends PlaybackEvent
case class PlaybackProgress(pos: Double) extends PlaybackEvent
