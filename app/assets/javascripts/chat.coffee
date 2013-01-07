class window.Chat
  constructor: (@main) ->

  message: (msg, who) =>
    console.log "chat: ", msg, who

  guestlistChanged: (users, listeners, lurkers) =>
    console.log "new guestlist: ", users, listeners, lurkers