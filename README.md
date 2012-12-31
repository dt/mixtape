# Mixtape

Mixtape is a web-based collaborative jukebox supporting with multiple 'rooms' and multiple listening and non-listening participants.
Mixtape is backed by Rdio, built on Playframework using webscokets.

![Screenshot](http://f.cl.ly/items/1n0K0R2A0P3n1z0A1X2t/Screen%20Shot%202012-12-30%20at%204.28.58%20PM.png)

## Prereqisits
* Play! 2.1 (currently in RC and has to be installed by hand [see below] -- 2.0.x has [a little NPE](https://github.com/playframework/Play20/pull/394))

### Installing Play 2.1-RC1 by hand
    brew update
    brew install play
    curl -O http://download.playframework.org/releases/play-2.1-RC1.zip
    unzip play-2.1-RC1.zip
    mv play-2.1-RC1 /usr/local/Cellar/play/2.1-RC1
    ln -sf /usr/local/Cellar/play/2.1-RC1/play /usr/local/bin/play

## Running the app
    play run
    open http://localhost:9000

## Notes
* Persistance is in-memory only for the moment, so restarting server clears all rooms/queues/etc
* You have to be logged in (via OpenID) to enqueue items, reorder the queue, vote up/down, etc.
* Items with net negitive balance of votes will be skipped when they reach the top of the queue

## License
Modified BSD (3-clause)