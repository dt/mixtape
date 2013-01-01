class Main
  constructor: (@room, rdioToken) ->
    @$content = $("#content")
    @queue = new Queue(this)
    @player = new Player(this, $("#player"), rdioToken)
    this.reconnect () =>
      @player.toggleLocalPlayback()
      this.reloadState()
    new Search(this, $("#searchterm"), $("#searchresults"))



  reloadState: =>
    jsRoutes.controllers.Application.queue(@room).ajax({success: (o) =>
      @queue.render(o.queue)
      @player.setPlaying(o.playing)
    })


  showArtist: (r) => jsRoutes.controllers.Rdio.artist(r).ajax({success: (o) => new Artist(this, o).show()})
  showAlbum: (a) => jsRoutes.controllers.Rdio.album(a).ajax({success: (o) => new Album(this, o).show()})

  openSocket: (f) =>
    console.log("opening socket")
    sock = new WebSocket(jsRoutes.controllers.Application.controls(@room).webSocketURL())
    sock.onmessage = this.handleMsg
    sock.onclose = () =>
      console.error "Socket Closed!"
      window.setTimeout (() => this.reconnect()), 15000
    sock.onopen = f
    sock
  reconnect: (f) =>
    @sock = this.openSocket () =>
      if f then f() else this.reloadState()

  handleMsg: (msg) =>
    o = JSON.parse msg.data
    console.log "recv: ", o unless o.event == "progress"
    switch o.event
      when "added" then  @queue.itemAdded(o.item)
      when "updated" then @queue.itemUpdated(o.item)
      when "skipped"  then @queue.itemSkipped(o.id)
      when "moved"  then @queue.itemMoved(o.id, o.nowBefore)
      when "started" then @player.start(o.item)
      when "progress" then @player.updatePosition(o.pos, o.ts)
      when "finished" then @player.setPlaying(false)
      when "start-broadcasting" then @player.setSendEvents(true)
      when "stop-broadcasting" then @player.setSendEvents(false)
      else
        this.reloadState()
        console.warn "unknown event: " + o.event, o

  send: (msg) =>
    console.log "send: ", msg unless msg.event == "progress"
    @sock.send JSON.stringify msg

  reportPlaybackFinished: =>
    this.send({event: "finished"})

jQuery ->
  window.mixtape = new Main(ROOM, RDIOTOKEN)