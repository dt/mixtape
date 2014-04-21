package controllers

import play.api._
import play.api.mvc._
import play.api.libs.openid._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import model.User

abstract class RequestWithUserOpt(userOpt: Option[User], request: Request[AnyContent]) extends WrappedRequest(request)
case class AuthenticatedRequest(user: User, request: Request[AnyContent]) extends RequestWithUserOpt(Some(user), request)
case class MaybeAuthenticatedRequest(userOpt: Option[User], request: Request[AnyContent]) extends RequestWithUserOpt(userOpt, request)

trait Secured {
  def user(implicit r: RequestHeader): Option[User] = for {
      id <- r.session.get("id")
      email <- r.session.get("email")
      first <- r.session.get("firstname")
      last <- r.session.get("lastname")
    } yield User(id, email, first, last)

  def Authenticated(f: AuthenticatedRequest => Future[SimpleResult]) = Action.async { implicit r =>
    user(r).map( u => f(AuthenticatedRequest(u, r))).getOrElse(Future(Results.Redirect(routes.Auth.start(r.uri))))
  }

  def MaybeAuthenticated(f: MaybeAuthenticatedRequest => Future[SimpleResult]) = Action.async { implicit r =>
    f(MaybeAuthenticatedRequest(user(r), r))
  }

}

object Auth extends GoogleAuthController

trait GoogleAuthController extends Controller {
  def start(returnTo: String) = Action.async { implicit r =>
    val url = "https://www.google.com/accounts/o8/id"
    val attributes = Seq(
      "email" -> "http://schema.openid.net/contact/email",
      "firstname" ->  "http://axschema.org/namePerson/first",
      "lastname" ->  "http://axschema.org/namePerson/last"
    )

    OpenID.redirectURL(url, routes.Auth.finish(returnTo).absoluteURL(), attributes).map(Redirect(_))
  }

  def finish(returnTo: String) = Action.async { implicit r =>
    {
      OpenID.verifiedId.map { info =>
        Redirect(returnTo).withSession(
          "id" -> java.util.UUID.randomUUID().toString(),
          Security.username -> info.attributes("email"),
          "email" -> info.attributes("email"),
          "firstname" -> info.attributes("firstname"),
          "lastname" -> info.attributes("lastname")
        )
      } recover { case t: Throwable => {
        Logger.warn("login failed?", t)
        Redirect(routes.Auth.start())
      }}
    }
  }

  def logout = Action { implicit r => Redirect("/").withNewSession }
}
