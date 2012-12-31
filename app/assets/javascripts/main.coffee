class Main
  constructor: (@room, rdioToken) ->
    @$content = $("#content")
    @queue = new Queue(this)
    @player = new Player(this, $("#player"), rdioToken)
    this.reconnect()
    new Search(this, $("#searchterm"), $("#searchresults"))

  reloadState: =>
    jsRoutes.controllers.Application.queue(@room).ajax({success: (o) =>
      @queue.render(o.queue)
      @player.setPlaying(o.playing)
    })


  showArtist: (r) => jsRoutes.controllers.Rdio.artist(r).ajax({success: (o) => new Artist(this, o).show()})
  showAlbum: (a) => jsRoutes.controllers.Rdio.album(a).ajax({success: (o) => new Album(this, o).show()})

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
    this.reloadState()

  handleMsg: (msg) =>
    o = JSON.parse msg.data
    console.log(o) unless o.event == "progress"
    switch o.event
      when "added" then  @queue.itemAdded(o.item)
      when "updated" then @queue.itemUpdated(o.item)
      when "skipped"  then @queue.itemSkipped(o.id)
      when "moved"  then @queue.itemMoved(o.id, o.nowBefore)
      when "started" then @player.start(o.item)
      when "progress" then @player.updatePosition(o.pos)
      when "finished" then @player.setPlaying(false)
      else
        @this.reloadState()
        console.warn "unknown event: " + o.event, o

  send: (msg) =>
    console.log "sending: ", msg unless msg.event == "progress"
    @sock.send JSON.stringify msg

  reportPlaybackFinished: =>
    this.send({event: "finished"})

  reportPlaybackPosition: (pos) =>
    this.send({event: "progress", pos: pos})

  localPlayback: true
  sendPlaybackEvents: false



jQuery ->
  window.mixtape = new Main(ROOM, RDIOTOKEN)