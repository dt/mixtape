package lib

import java.security.MessageDigest

object MD5 {
  val digest = MessageDigest.getInstance("MD5")
  def apply(str: String) = digest.digest(str.getBytes).map("%02x".format(_)).mkString
}