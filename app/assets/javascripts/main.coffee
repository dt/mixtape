class Main
  @sock
  constructor: (@room, @$content) ->
    @queue = new Queue(this)
    this.reconnect()

  openSocket: =>
    console.log("opening socket")
    sock = new WebSocket(jsRoutes.controllers.Application.controls(@room).webSocketURL())
    sock.onmessage = this.handleMsg
    sock.onclose = () =>
      console.error "Socket Closed!"
      window.setTimeout (() => this.reconnect()), 15000
    sock

  reconnect: =>
    @sock = this.openSocket()
    @queue.reload()

  handleMsg: (msg) =>
    o = JSON.parse msg.data
    switch o.event
      when "added" then  @queue.itemAdded(o.item)
      when "updated" then @queue.itemUpdated(o.item)
      when "skipped"  then @queue.itemSkipped(o.id)
      when "moved"  then @queue.itemMoved(o.id, o.nowBefore)
      else
        @queue.reload()
    console.log o

  send: (msg) =>
    @sock.send JSON.stringify msg

  showArtist: (r) =>
    jsRoutes.controllers.Rdio.artist(r).ajax({success: (o) => new Artist(this, o).show()})

  showAlbum: (a) =>
    jsRoutes.controllers.Rdio.album(a).ajax({success: (o) => new Album(this, o).show()})

  playbackStarted: (id) =>
    this.send({event: "started", id: id})

  playbackFinished: (id) =>
    this.send({event: "finished"})


jQuery ->
  window.mixtape = new Main(ROOM, $("#content"))
  new Search(window.mixtape, $("#searchterm"), $("#searchresults"))

  $(".sidebar-nav a").click (c) ->
    socket.sendMsg({'clicked': $(this).index(".sidebar-nav a")})
