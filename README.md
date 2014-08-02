# Mixtape

Mixtape is a web-based collaborative jukebox supporting with multiple 'rooms' and multiple listening and non-listening participants.
Mixtape is backed by Rdio, built on Playframework using websockets.

![Screenshot](http://f.cl.ly/items/0X3B1z073m0E2d1p3C16/Screen%20Shot%202013-01-03%20at%2010.18.23%20PM.png)

## Prerequisites
Play 2.1

### Installing Play 2.1
OSX:
    brew install play
or look at [http://www.playframework.com/documentation/2.1.1/Installing]

## Running the app
    play run
    open http://localhost:9000

## Notes
* Persistance is in-memory only for the moment, so restarting server clears all rooms/queues/etc
* You have to be logged in (via OpenID) to enqueue items, reorder the queue, vote up/down, etc.
* Items with net negitive balance of votes will be skipped when they reach the top of the queue

## License
Modified BSD (3-clause)
