package model

import scala.collection.mutable

case class Votes(up: mutable.Set[User] = mutable.Set.empty, down: mutable.Set[User] = mutable.Set.empty)
case class QueueItem(track: Track, by: User, votes: Votes = Votes())

trait ItemEvent

case class ItemAdded(item: QueueItem) extends ItemEvent
case class ItemUpdated(item: QueueItem) extends ItemEvent
case class ItemMoved(item: String, nowBefore: String) extends ItemEvent

case class PlaybackStarted(id: String) extends ItemEvent
case class PlaybackPaused(id: String) extends ItemEvent
case class PlaybackFinished(id: String) extends ItemEvent
case class PlaybackSkipped(id: String) extends ItemEvent

