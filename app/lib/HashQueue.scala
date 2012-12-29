package lib

import scala.collection.mutable.LinkedHashMap

class HashQueue[A, B] extends LinkedHashMap[A, B] {
  def pop(): Option[B] = Option(this.firstEntry).map { e =>
    this.remove(e.key)
    e.value
  }
  def push(e: (A, B)*) = this.++=(e)

  def keyAfter(key: A) = Option(findEntry(key)).flatMap(e => Option(e.later).map(_.key))

  def moveTo(key: A, putBeforeKey: A) {
    val elem = Option(findEntry(key)).getOrElse(throw new NoSuchElementException(key.toString))
    val putBefore = Option(findEntry(putBeforeKey)).getOrElse(throw new NoSuchElementException(putBeforeKey.toString))

    // pull elem out and reconnect next/prev's
    Option(elem.earlier).foreach(_.later = elem.later)
    Option(elem.later).foreach(_.earlier = elem.earlier)
    if (this.firstEntry eq elem)
      this.firstEntry = elem.later
    if (this.lastEntry eq elem)
      this.lastEntry = elem.earlier

    // set elem's new next/prev
    elem.earlier = putBefore.earlier
    elem.later = putBefore

    // splice elem into new location
    Option(putBefore.earlier).foreach(_.later = elem)
    putBefore.earlier = elem
    if (this.firstEntry eq putBefore)
      this.firstEntry = elem

  }
}