class window.Track
  this.format = (d) ->
    sec = d % 60
    sec = "0" + sec if sec < 10
    Math.floor(d / 60) + ":" + sec