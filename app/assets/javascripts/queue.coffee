class window.Queue
  this.id = (id) -> "enqueued-" + id

  constructor: (@main) ->
    @$queue = $("#queue")
    @$queue.sortable({update: (e, ui) =>
      this.moveItem(ui.item.data("id"), ui.item.next().data("id"))
    });
    @$queue.disableSelection();

  addTrack: (id, name, artist, artistKey, album, albumKey, art, duration) => @main.send({
    event: "add",
    id: id,
    name: name,
    artist: artist,
    artistKey: artistKey,
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
  itemMoved: (id, nowBefore) => this.findOrReload id, ($r) =>
    if nowBefore then $r.insertBefore($("#" + Queue.id(nowBefore))) else @$queue.append($r)

  itemPlayed: (id) => this.findOrReload(id, ($r) => $r.slideUp(250, () => $r.remove()))
  itemSkipped: (id) => this.findOrReload id, ($r) =>
    $r.hide "slide", {"direction": "right"}, 200, (e) => $r.remove()

  renderItem: (res) =>
    this.fillItem(res, $("<div>").addClass("enqueued").attr("id", Queue.id(res.id)).data("id", res.id))

  @renderUsers: (res) =>
    userDiv = $("<div>").addClass("votes")
    userDiv.append(Queue.renderUser(res.by))
    res.votes.up.map (u) =>
      userDiv.append(Queue.renderUser(u))
    userDiv

  @renderUser: (u) =>
    colorCode = parseInt(u.id.replace(/[a-z]/g, "").replace(/-/g, "").substr(10)) % 9
    initials = u.firstname[0] + u.lastname[0]
    initialsDiv = $("<div>").addClass("initials").addClass("reversed").addClass("color-" + colorCode)
    initialsInner = $("<div>").addClass("initials-text")
      .text(initials)
      .attr("rel", "tooltip")
      .attr("title", u.firstname + " " + u.lastname + " (" + u.email + ")")
    initialsDiv.append(initialsInner)
    initialsInner.qtip();
    initialsDiv

  fillItem: (res, $r) =>
    $r.toggleClass "skipping", res.skipping == true
    $tdata = $("<div>").addClass("trackdata")
    $r.append $tdata

    $tdata.append $("<img>").addClass("icon").attr("src", res.track.icon).click (e) =>
      e.stopPropagation()
      @main.showAlbum(res.track.albumKey)

    $tdata.append $("<span>").addClass("name").text(res.track.name)
    $from = $("<div>").addClass("details")
    $tdata.append $from

    $from.append $("<a>").addClass("trackartist").text(res.track.artist).click (e) =>
      e.stopPropagation()
      @main.showArtist(res.track.artistKey)
    $from.append $("<a>").addClass("trackalbum").text(res.track.album).click (e) =>
      e.stopPropagation()
      @main.showAlbum(res.track.albumKey)
    $r.append $("<a>").addClass("vote-up").click () => this.voteUp(res.id)
    $r.append $("<a>").addClass("vote-down").click () => this.voteDown(res.id)

    $r.append(Queue.renderUsers(res))

    $r.append $("<div>").addClass("clearfix")
    $r

class window.Player
  localPlayback: false
  sendPlaybackEvents: false

  constructor: (@main, @$player, token, f) ->
    $("#local-playback").click this.toggleLocalPlayback
    $("#playback-master").click this.toggleSendEvents
    this.renderSendEventsState()
    this.renderLocalPlaybackState()
    @totalCorrection = 0
    @rdio = this.setupRdio(@$player.find(".rdio"), token, f)
    $("#playback-lag").click this.resyncPosition

  setupRdio: ($rdio, token, f) =>
    $rdio.bind "ready.rdio", (_, userinfo) =>
      console.log("rdio is ready.")
      if (userinfo && userinfo.isSubscriber)
        console.log "rdio subscriber! good!", userinfo
        @canPlayLocally = true
        $("#local-playback").addClass("clickable")
      else
        @canPlayLocally = false
        console.warn("you need to be logged in to rdio", userinfo)
      f() if f != undefined

    $rdio.bind "playingTrackChanged.rdio", (e, playingTrack, sourcePosition) =>

    $rdio.bind "positionChanged.rdio", (e, pos) => this.localPlaybackPositionChanged(pos)

    $rdio.bind "playStateChanged.rdio", (e, newState) =>
      if @playbackState == 1 && newState == 2 && !@skipping
        this.localPlaybackFinished()
      @skipping = false
      @playbackState = newState
    console.log("setting up rdio...")
    $rdio.rdio(token)

  start: (item) =>
    this.setPlaying(item)
    if (@localPlayback && @playing)
      console.log("telling rdio to play", @playing.track.id, @totalCorrection)
      @rdio.play(@playing.track.id, {initialPosition: @totalCorrection})
    @main.queue.itemPlayed(item.id)

  skip: (id) =>
    display = @$player.find("#playing")
    if display.data("id") == id
      display.hide()

  setPlaying: (item) =>
    @skipping = @playbackState == 1
    @playing = item
    display = @$player.find("#playing")
    if @playing
      display.data("id", item.id)
      @playing.ts = this.timestamp()
      @$player.addClass("playing")
      this.renderPlaybackPosition()
      display.find(".name").text(@playing.track.name)
      display.find(".icon").attr("src", @playing.track.icon)
      display.find(".votes").empty().append(Queue.renderUsers(@playing))
      display.find(".trackartist").text(@playing.track.artist).unbind("click").click (e) =>
        e.stopPropagation()
        @main.showArtist(@playing.track.artistKey)
      display.find(".vote-up").unbind("click").click () => @main.queue.voteUp(@playing.id)
      display.find(".vote-down").unbind("click").click () => @main.queue.voteDown(@playing.id)
      display.show()
    else
      @rdio.stop() if @localPlayback
      display.hide()
      @$player.removeClass("playing")
      display.data("id", "")

  timestamp: -> new Date().getTime()

  localPlaybackFinished: =>
    console.log "playback finished", @playing
    @main.send({event: "finished", id: @playing.id}) if @sendPlaybackEvents

  localPlaybackPositionChanged: (pos) =>
    now = this.timestamp()
    if @sendPlaybackEvents then @main.send({event: "progress", pos: pos, ts: now})
    @playing.localPosition = pos
    @playing.localPositionAsOf = now
    if @localPlayback && @playing
      predicted = (@playing.position + (now - @playing.positionAsOf)/1000)
      $("#playback-lag").text((if predicted > pos then "-" else "+") + Math.abs(predicted - pos).toFixed(3))

  resyncPosition: =>
    now = this.timestamp()
    if @playing
      localPredicted = (@playing.localPosition + (now - @playing.localPositionAsOf)/1000)
      groupPredicted = (@playing.position + (now - @playing.positionAsOf)/1000)
      lag = groupPredicted - localPredicted
      correction = @totalCorrection + lag
      @totalCorrection = correction
      $("#total-correction").text(@totalCorrection.toFixed(4))
      target = groupPredicted + correction
      console.log("correcting playback position", target - localPredicted, correction, @totalCorrection)
      @rdio.seek(target)

  updatePosition: (pos, time) =>
    if @playing
      @playing.position = pos
      @playing.positionAsOf = time
    this.renderPlaybackPosition()

  renderPlaybackPosition: =>
    if @playing
      @$player.find(".progress .bar").css("width", Math.floor(100 * @playing.position / @playing.track.duration)+"%")
      $("#position").text(Track.formatDuration(Math.round(@playing.position)))

  toggleSendEvents: () => this.setSendEvents(!@sendPlaybackEvents)

  setSendEvents: (enabled) =>
    was = @sendPlaybackEvents
    @sendPlaybackEvents = @localPlayback && !!enabled
    this.renderSendEventsState()

    if @sendPlaybackEvents
      this.notifyServerBroadcasting()
    else if was && !@sendPlaybackEvents
       @main.send({"event": "stopped-broadcasting"})

  renderSendEventsState: =>
    $("#playback-master").toggleClass("disabled", !@sendPlaybackEvents).toggleClass("clickable", @localPlayback)

  toggleLocalPlayback: () => this.setLocalPlayback(!@localPlayback)

  setLocalPlayback: (enabled) =>
    was = @localPlayback
    @localPlayback = @canPlayLocally && enabled
    this.renderLocalPlaybackState()
    if not @localPlayback
      this.setSendEvents(false)
    else
      this.notifyServerListening()

    if was && !@localPlayback
      @main.send({"event": "stopped-listening"})
      @rdio.stop()

    else if @localPlayback && !was
      if @playing
        @rdio.play @playing.track.id, {initialPosition: @playing.position}

  notifyServerListening: =>
    @main.send({"event": "listening"}) if @localPlayback

  notifyServerBroadcasting: =>
    @main.send({"event": "broadcasting"}) if @sendPlaybackEvents


  renderLocalPlaybackState: => $("#local-playback").toggleClass("disabled", !@localPlayback)
