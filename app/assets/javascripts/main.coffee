class Main
  constructor: (@sock, @$content) ->

  showArtist: (r) =>
    jsRoutes.controllers.Rdio.artist(r).ajax({success: (o) => new Artist(this, o).show()})

  showAlbum: (a) =>
    jsRoutes.controllers.Rdio.album(a).ajax({success: (o) => new Album(this, o).show()})

  addTrack: (id, name, artist, album, albumKey, art) =>
    @sock.sendMsg({
      event: "add",
      id: id,
      name: name,
      artist: artist,
      album: album,
      albumKey: albumKey,
      art: art
    })

  moveItem: (id, putBefore) =>
    @sock.sendMsg({event: "move", id: id, putBefore: putBefore})

  voteUp: (id) =>
    @sock.sendMsg({event: "voteup", id: id})

  voteDown: (id) =>
    @sock.sendMsg({event: "votedown", id: id})

  playbackStarted: (id) =>
    @sock.sendMsg({event: "started", id: id})

  playbackFinished: (id) =>
    @sock.sendMsg({event: "finished"})


jQuery ->
  console.log("Room:", ROOM);
  socket = new WebSocket(jsRoutes.controllers.Application.controls(ROOM).webSocketURL())
  socket.onmessage = (msg) ->
    o = JSON.parse msg.data
    console.log o
  socket.onclose = () ->
    console.error "Socket Closed!"
  socket.sendMsg = (msg) ->
    socket.send JSON.stringify msg

  window.mixtape = new Main(socket, $("#content"))
  new Search(window.mixtape, $("#searchterm"), $("#searchresults"))

  $(".sidebar-nav a").click (c) ->
    socket.sendMsg({'clicked': $(this).index(".sidebar-nav a")})
