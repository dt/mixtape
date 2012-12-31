package controllers

import play.api._
import play.api.mvc._
import play.api.libs.openid._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import model.User

abstract class RequestWithUserOpt(userOpt: Option[User], request: Request[AnyContent]) extends WrappedRequest(request)
case class AuthenticatedRequest(user: User, request: Request[AnyContent]) extends RequestWithUserOpt(Some(user), request)
case class MaybeAuthenticatedRequest(userOpt: Option[User], request: Request[AnyContent]) extends RequestWithUserOpt(userOpt, request)

trait Secured {
  def user(implicit r: RequestHeader): Option[User] = for {
      email <- r.session.get("email")
      first <- r.session.get("firstname")
      last <- r.session.get("lastname")
    } yield User(email, first, last)

  def Authenticated(f: AuthenticatedRequest => Result) = Action { implicit r =>
    user(r).map( u => f(AuthenticatedRequest(u, r))).getOrElse(Results.Redirect(routes.Auth.start))
  }

  def MaybeAuthenticated(f: MaybeAuthenticatedRequest => Result) = Action { implicit r =>
    f(MaybeAuthenticatedRequest(user(r), r))
  }

}

object Auth extends Controller {

  def start = Action { implicit request =>
    val url = "https://www.google.com/accounts/o8/id"

    val attributes = Seq(
      "email" -> "http://schema.openid.net/contact/email",
      "firstname" ->  "http://axschema.org/namePerson/first",
      "lastname" ->  "http://axschema.org/namePerson/last"
    )

    AsyncResult { OpenID.redirectURL(url, routes.Auth.finish.absoluteURL(), attributes).map(Redirect(_)) }
  }



  def finish = Action { implicit request =>
    AsyncResult {
      OpenID.verifiedId.map { info =>
        Redirect(routes.Application.index("")).withSession(
          Security.username -> info.attributes("email"),
          "email" -> info.attributes("email"),
          "firstname" -> info.attributes("firstname"),
          "lastname" -> info.attributes("lastname")
        )
      } recover { case t: Throwable => {
        Logger.warn("login failed?", t)
        Redirect(routes.Auth.start)
      }}
    }
  }
}
