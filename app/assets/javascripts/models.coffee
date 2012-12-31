class window.Artist
  show: =>

  this.renderSearchResult = (res, click) ->
    $r = $("<li>").addClass("result").addClass("artist").click () => click(res.key)
    $r.append $("<img>").addClass("icon").attr("src", res.icon)
    $r.append $("<span>").addClass("name").text(res.name)
    $r.append $("<div>").addClass("details").append $("<div>").addClass("albumCount").text(res.albumCount)
    $r.append $("<div>").addClass("clearfix")
    $r

class window.Album
  constructor: (@main, @a) ->
    @summary = $('<div>').addClass('summary')
    img = $('<img>').attr('src', @a.icon).attr('alt', @a.name)
    details = $('<div>').addClass('details')
    details.append $('<h1>').text(@a.name)
    details.append $('<h2>').text(@a.artist)
    details.append $('<h3>').text(@a.displayDate)
    @summary.append(img, details)
    @tracks = $('<table>').addClass('tracks').append @a.tracks.map (t) =>
      row = $('<tr>')
      row.append $('<td>').text(t.trackNum)
      row.append $('<td>').text(t.name)
      row.append $('<td>').text(t.artist).addClass("trackartist")
      row.append $('<td>').text(Track.format(t.duration))
      row.click () => @main.queue.addTrack(t.key, t.name, t.artist, t.album, t.albumKey, t.icon, t.duration)
      row

  show: => @main.$content.empty().append $('<div>').addClass('album').append(@summary, @tracks)

  this.renderSearchResult = (res, click, artistClick) ->
    $r = $("<li>").addClass("result").addClass("album").click () => click(res.key)
    $r.append $("<img>").addClass("icon").attr("src", res.icon)
    $r.append $("<span>").addClass("name").text(res.name)
    $r.append $("<div>").addClass("details").append $("<a>").addClass("albumartist").text(res.artist).click (e) =>
      e.stopPropagation()
      artistClick(res.artistKey)
    $r.append $("<div>").addClass("clearfix")
    $r

class window.Track
  this.format = (d) -> Math.floor(d / 60) + ":" + ("0"+(d % 60)).slice(-2)

  this.renderSearchResult = (res, click, artistClick, albumClick) =>
    $r = $("<li>").addClass("result").addClass("track").click () =>
      click(res.key, res.name, res.artist, res.album, res.albumKey, res.icon, res.duration)
    $r.append $("<img>").addClass("icon").attr("src", res.icon)
    $r.append $("<span>").addClass("name").text(res.name)
    $from = $("<div>").addClass("details")
    $r.append $from
    $from.append $("<a>").addClass("trackartist").text(res.artist).click (e) =>
      e.stopPropagation()
      artistClick(res.artistKey)
    $from.append $("<a>").addClass("trackalbum").text(res.album).click (e) =>
      e.stopPropagation()
      albumClick(res.albumKey)
    $r.append $("<div>").addClass("clearfix")
    $r