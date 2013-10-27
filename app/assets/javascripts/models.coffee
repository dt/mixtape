class window.Artist
  compareByDate: (a,b) ->
    if a.releaseDate > b.releaseDate
       return -1
    if a.releaseDate < b.releaseDate
      return 1
    return 0

  constructor: (@main, @a) ->
    @summary = $('<div>').addClass('summary')
    details = $('<div>').addClass('details')
    details.append $('<h1>').text(@a.artistInfo.name)

    if @a.artistInfo.icon.indexOf('no-artist') == -1
      img = $('<img>').attr('src', @a.artistInfo.icon).attr('alt', @a.artistInfo.name)
      @summary.append(img)

    albums = @a.albums.sort(@compareByDate)

    @summary.append(details)
    @albums = $('<table>').addClass('albums').append albums.map (t) =>
      img = $('<img>').attr('src', t.icon).attr('alt', t.name)
      row = $('<tr>')
      row.append $('<td>').append(img)
      /* TODO(blackmad): make this prettier */
      row.append $('<td>').html(t.name + "<br><br>" + t.displayDate + "<br>" + t.length + " tracks")
      row.click () => @main.showAlbum(t.key)
      row

  show: => @main.$content.empty().append $('<div>').addClass('artist').append(@summary, @albums)

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
    imgDiv = $('<div>').addClass('albumArt')
    img = $('<img>').attr('src', @a.icon).attr('alt', @a.name)
    imgDiv.append($('<div>').addClass('playAll'))
    imgDiv.append(img)
    imgDiv.click () =>
      @a.tracks.map (t) =>
        @main.queue.addTrack(t.key, t.name, t.artist, t.artistKey, t.album, t.albumKey, t.icon, t.duration)
    details = $('<div>').addClass('details')
    details.append $('<h1>').text(@a.name)
    details.append $('<h2>').text(@a.artist)
    details.append $('<h3>').text(@a.displayDate)
    @summary.append(imgDiv, details)
    @tracks = $('<table>').addClass('tracks').append @a.tracks.map (t) =>
      row = $('<tr>')
      row.append $('<td>').text(t.trackNum)
      row.append $('<td>').text(t.name)
      row.append $('<td>').text(t.artist).addClass("trackartist")
      row.append $('<td>').text(Track.formatDuration(t.duration))
      row.click () => @main.queue.addTrack(t.key, t.name, t.artist, t.artistKey, t.album, t.albumKey, t.icon, t.duration)
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
  this.formatDuration = (d) -> Math.floor(d / 60) + ":" + ("0"+(d % 60)).slice(-2)

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
