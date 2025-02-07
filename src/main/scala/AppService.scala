package uniso.app

import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.StatusCodes._
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers._
import org.apache.pekko.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server._
import org.apache.pekko.http.scaladsl.server.directives.AuthenticationDirective
import dto.user_principal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Encoding, Schema}
import io.swagger.v3.oas.annotations.parameters._
import io.swagger.v3.oas.annotations.enums._
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.tresql._
import org.wabase.Authentication.{BasicAuth, Crypto}
import org.wabase._
import spray.json._
import uniso.app.biz.UserManager
import jakarta.ws.rs.{Consumes, GET, POST, Path, Produces}
import jakarta.ws.rs.core.MediaType
import io.swagger.v3.oas.annotations.headers.Header
import spray.json.DefaultJsonProtocol._

import java.text.SimpleDateFormat
import java.util.Locale
import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.language.reflectiveCalls

object AppService
  extends Execution
  with AppServiceBase[user_principal]
  with AppFileServiceBase[user_principal]
  with AppConfig
  with AppVersion
  with DefaultAppExceptionHandler[user_principal]
  with DefaultWsInitialEventsPublisher
  with ServerNotifications
  with Authentication[user_principal]
  with JsonSessionEncoder[user_principal]
  with DeferredControl
  with NoServerStatistics
  with SprayJsonSupport

  with Loggable {

  {
    java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider)
  }

  override type App = App.type

  override def initApp: App = App

  override protected def initFileStreamer = App

  override def initDeferredStorage = new DbDeferredStorage(appConfig, App, this)

  override def execution = uniso.app.execution

  override lazy val deferredUris = Set("long-req")
  override lazy val deferredTimeouts = Map("long-req" -> 10.seconds)
  override lazy val deferredWorkerCount = 3

  override val appVersion =
    Option(this.getClass.getResourceAsStream("/version.txt"))
      .map(scala.io.Source.fromInputStream(_).mkString)
      .map(_.trim)
      .filter(_ != "")
      .getOrElse("development version " + (new SimpleDateFormat("yyyy.MM.dd HH:mm:ss")).format(new java.util.Date))

  override implicit val userFormat: RootJsonFormat[user_principal] = UserHelper.userFormat
  lazy val anonymousUser = UserHelper.anonymous

  override val SignedInDefaultPath = "/api/current_user"

  override def remoteAddressToString(ra: RemoteAddress): String = ra.toIP.map(_.ip.getHostAddress).orNull // FIXME to core

  case class LoginRequest(username: Option[String], password: Option[String])
  implicit val loginRequestFormat: RootJsonFormat[LoginRequest] = jsonFormat2(LoginRequest)

  override def signInUser: AuthenticationDirective[user_principal] = {
    val dr: Directive1[Option[user_principal]] = (entity(as[LoginRequest]) & extractClientIP.map(remoteAddressToString) & extractUserAgent).tflatMap {case (userInfo, ip, ua) =>
      val username = userInfo.username.getOrElse(throw new IllegalArgumentException("username is required")).toLowerCase
      val password = userInfo.password.getOrElse(throw new IllegalArgumentException("password is required"))
      onSuccess(UserManager.authenticateUser(username, password, ip, ua))
    }

    dr.flatMap {
      case Some(user) if user != null => provide(user)
      case _ => reject(AuthenticationFailedRejection(CredentialsRejected, AppDefaultChallenge)): Directive1[user_principal]
    }
  }

  override def authenticateUser: AuthenticationDirective[Option[user_principal]] = {
    (super.authenticateUser: AuthenticationDirective[Option[user_principal]]) | provide(Option(anonymousUser))
  }

  override def authFailureRoute: Route = complete(StatusCodes.Unauthorized)

  @POST
  @Path("/api/login")
  @Operation(
    summary = "Login service",
    description = "Creates a session for the user",
    requestBody = new RequestBody(content = Array(new Content(mediaType = "application/json", schema = new Schema(implementation = classOf[LoginRequest])))),
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Logged in user", content = Array(new Content(schema = new Schema(implementation = classOf[user_principal])))),
      new ApiResponse(responseCode = "401", description = "Unauthorized, login failed"),
      new ApiResponse(responseCode = "500", description = "Internal Server Error, username or password is required")
    )
  )
  def login = (path("api" / "login") & post) {
    signIn
  }

  @GET
  @Path("/api/current_user")
  @Produces(Array(MediaType.APPLICATION_JSON))
  @Operation(
    summary = "Current user service",
    description = "Returns the current user",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "Logged in user", content = Array(new Content(schema = new Schema(implementation = classOf[user_principal]))))
    )
  )
  def currentUser = (path("api" / "current_user") & authenticateUser) { u =>
    complete(u)
  }

  @GET
  @Path("/api/logout")
  @Operation(
    summary = "Logout service",
    description = "Ends the session for the user",
    parameters = Array(
      new Parameter(name = "redirectUrl", in = ParameterIn.QUERY, required = false, description = "URL to redirect to after logout", schema = new Schema(implementation = classOf[String]))
    ),
    responses = Array(
      new ApiResponse(responseCode = "303", description = "Redirects to redirect url", headers = Array(new Header(name = "Location", description = "Redirect URL from parameter")))
    )
  )
  def logout = (path("api" / "logout") & parameters("redirectUrl".withDefault("")) & authenticateUser) { (redirectUrl, u) =>
    deleteCookie(SessionCookieName, path = "/") {
      u match {
        case Some(user) if user.name != "anonymous" =>
          App.auditLogout(user)
          redirect(s"/$redirectUrl", StatusCodes.SeeOther)
        case _ =>
          redirect(s"/$redirectUrl", StatusCodes.SeeOther)
      }
    }
  }

  val prettyExceptionHandler = appExceptionHandler.withFallback(ExceptionHandler({ case scala.util.control.NonFatal(e) =>
    logger.error(e.getMessage, e)
    complete(HttpResponse(InternalServerError, entity = e.getMessage))
  }))


  def route =  {
    handleExceptions(prettyExceptionHandler) {
      SwaggerDocService.routes ~
      login ~
      logout ~
      currentUser ~
      authenticate { implicit user =>
        applicationState { implicit state =>
          uploadPath { uploadAction } ~
            path("download" / LongNumber / Segment) { (id, sha256) => downloadAction(id, sha256) }
        } ~
        crudPath { crudAction } ~
        apiPath { apiAction } ~
        metadataPath { viewName => applicationState { implicit state => metadataAction(viewName) } } ~
        countPath { viewName => applicationState { implicit state => extractTimeout { implicit timeout => countAction(viewName) } } } ~
        deferredRequestPath { hash => deferredHttpRequestAction(hash, user.id.toString) } ~
        deferredResultPath { hash => deferredResultAction(hash, user.id.toString) }
      }
    }
  }
}
