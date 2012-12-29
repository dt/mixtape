class window.Search
  constructor: (@main, @$in, @$res) ->
    $in.keypress this.search

  search: (c) =>
    if c.keyCode == 13
      jsRoutes.controllers.Rdio.search(@$in.val()).ajax({success: this.populateResutls})

  populateResutls: (resp) =>
    divs = resp.result.results.map this.renderResult
    @$res.empty().append(divs)

  renderResult: (res) =>
    $r = $("<li>").addClass "result"

    if res.icon
      $r.append $("<img>").addClass("icon").attr("src", res.icon)
    $r.append $("<span>").addClass("name").text(res.name)
    $from = $("<div>").addClass("details")
    $r.append $from
    switch res.type
      when "r"
        $r.click () => @main.showArtist(res.key)
        $from.append $("<div>").addClass("albumCount").text(res.albumCount)
        $r.addClass "artist"
      when "a"
        $r.click () => @main.showAlbum(res.key)
        $from.append $("<a>").addClass("albumartist").text(res.artist).click (e) =>
          e.stopPropagation()
          @main.showArtist(res.artistKey)
        $r.addClass "album"
      when "t"
        $r.click () => @main.addTrack(res.key, res.name, res.artist, res.album, res.albumKey, res.icon)
        $from.append $("<a>").addClass("trackartist").text(res.artist).click (e) =>
          e.stopPropagation()
          @main.showArtist(res.artistKey)
        $from.append $("<a>").addClass("trackalbum").text(res.album).click (e) =>
          e.stopPropagation()
          @main.showAlbum(res.albumKey)
        $r.addClass "track"


    $r.append $("<div>").addClass("clearfix")
    $r