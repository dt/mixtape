class window.Queue
  this.id = (id) ->
      "enqueued-" + id

  constructor: (@main) ->
    @$queue = $("#queue")
    @$queue.sortable({update: (e, ui) =>
      this.moveItem(ui.item.data("id"), ui.item.next().data("id"))
    });
    @$queue.disableSelection();


  addTrack: (id, name, artist, album, albumKey, art, duration) =>
    @main.send({
      event: "add",
      id: id,
      name: name,
      artist: artist,
      album: album,
      albumKey: albumKey,
      icon: art,
      duration: duration
    })

  moveItem: (id, putBefore) => @main.send({event: "move", id: id, putBefore: putBefore})

  voteUp: (id) => @main.send({event: "voteup", id: id})
  voteDown: (id) => @main.send({event: "votedown", id: id})

  render: (r) => @$queue.empty().append(r.map(this.renderItem))

  findOrReload: (id, f) =>
    $r = $("#" + Queue.id(id))
    if $r then f($r) else @main.reloadState()

  itemAdded: (item) => @$queue.append(this.renderItem(item))
  itemUpdated: (item) => this.findOrReload(item.id, ($r) => this.fillItem(item, $r.empty()))
  itemMoved: (id, nowBefore) => this.findOrReload(id, ($r) =>
    if nowBefore then $r.insertBefore($("#" + Queue.id(nowBefore))) else @$queue.append($r)
  )

  itemPlayed: (id) => this.findOrReload(id, ($r) => $r.remove())
  itemSkipped: (id) => this.findOrReload(id, ($r) => $r.remove())

  renderItem: (res) =>
    this.fillItem(res, $("<div>").addClass("enqueued").attr("id", Queue.id(res.id)).data("id", res.id))

  fillItem: (res, $r) =>
    $r.append $("<img>").addClass("icon").attr("src", res.track.icon).click (e) =>
      e.stopPropagation()
      @main.showAlbum(res.track.albumKey)

    $r.append $("<span>").addClass("name").text(res.track.name)
    $from = $("<div>").addClass("details")
    $r.append $from

    $from.append $("<a>").addClass("trackartist").text(res.track.artist).click (e) =>
      e.stopPropagation()
      @main.showArtist(res.track.artistKey)
    $from.append $("<a>").addClass("trackalbum").text(res.track.album).click (e) =>
      e.stopPropagation()
      @main.showAlbum(res.track.albumKey)
    $r.append $("<div>").addClass("clearfix")
    $r

class window.Player
  constructor: (@main, @$player, token) ->
    @$player.find("#mode .local-playback").click this.toggleLocalPlayback
    @$player.find("#mode .playback-master").click this.toggleSendEvents
    this.renderSendEventsState()
    this.renderLocalPlaybackState()

    @$rdio = @$player.find('.rdio')
    @$rdio.bind 'ready.rdio', (_, userinfo) =>
      console.log "rdio is ready", userinfo

    @$rdio.bind 'playingTrackChanged.rdio', (e, playingTrack, sourcePosition) =>

    @$rdio.bind 'positionChanged.rdio', (e, pos) =>
      @main.reportPlaybackPosition(pos) if @main.sendPlaybackEvents

    @$rdio.bind 'playStateChanged.rdio', (e, newState) =>
      if @playbackState == 1 && newState == 2
        @main.reportPlaybackFinished() if @main.sendPlaybackEvents
      @playbackState = newState


    console.log("starting rdio...")
    @rdio = @$rdio.rdio(token)

  start: (item) =>
    console.log("now playing: ", item)
    this.setPlaying(item)
    if (@main.localPlayback)
      @rdio.play(item.track.id)
    @main.queue.itemPlayed(item.id)

  setPlaying: (item) =>
    @playing = item
    if @playing
      @$player.addClass("playing")
      this.renderPlaybackPosition()
      @$player.find(".name").text(@playing.track.name)
      @$player.find(".icon").attr("src", @playing.track.icon)
      @$player.find(".trackartist").text(@playing.track.artist).click (e) =>
        e.stopPropagation()
        @main.showArtist(@playing.track.artistKey)
    else
      @$player.removeClass("playing")


  updatePosition: (pos) =>
    if @playing
      @playing.position = pos
    this.renderPlaybackPosition()

  renderPlaybackPosition: =>
    if @playing
      @$player.find('.progress .bar').css('width', Math.floor(100 * @playing.position / @playing.track.duration)+'%')

  toggleSendEvents: () =>
    @main.sendPlaybackEvents = ! @main.sendPlaybackEvents
    this.renderSendEventsState()

  renderSendEventsState: =>
    @$player.find("#mode .playback-master").toggleClass('disabled', !@main.sendPlaybackEvents)

  toggleLocalPlayback: () =>
    @main.localPlayback = ! @main.localPlayback
    this.renderLocalPlaybackState()
    if (!@main.localPlayback)
      @main.sendPlaybackEvents = false
      this.renderSendEventsState()
      @rdio.stop()
    else
      if @playing
        @rdio.play @playing.track.id, {initialPosition: @playing.position}

  renderLocalPlaybackState: =>
    @$player.find("#mode .local-playback").toggleClass('disabled', !@main.localPlayback)


