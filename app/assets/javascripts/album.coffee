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
      row.append $('<td>').text(t.artist)
      row.append $('<td>').text(Track.format(t.duration))
      row.click () => @main.addTrack(t.key, t.name, t.artist, t.album, t.albumKey, t.icon)
      row

  show: =>
    @main.$content.empty().append $('<div>').addClass('album').append(@summary, @tracks)