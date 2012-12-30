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
    switch res.type
      when "r"
        Artist.renderSearchResult(res, @main.showArtist)
      when "a"
        Album.renderSearchResult(res, @main.showAlbum, @main.showArtist)
      when "t"
        Track.renderSearchResult(res, @main.queue.addTrack, @main.showArtist, @main.showAlbum)
