package lib

object Gravatar {
  def apply(email: String): String = "http://www.gravatar.com/avatar/%s?d=mm".format(MD5(email))
  def apply(email: String, size: Int): String = apply(email) + "&s=" + size
}