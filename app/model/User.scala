package model

import lib.Gravatar

case class User(id: String, email: String, firstname: String, lastname: String) {
  override def equals(o: Any) = o match { case u: User => email == u.email; case _ => false }
  override def hashCode() = email.hashCode

  def avatar = Gravatar(email)
  def avatar(s: Int) = Gravatar(email, s)
}