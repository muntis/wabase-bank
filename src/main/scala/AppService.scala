package uniso.app

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.AuthenticationDirective
import dto.{user_principal}
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.{ArraySchema, Content, Encoding, Schema}
import io.swagger.v3.oas.annotations.parameters._
import io.swagger.v3.oas.annotations.enums._
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.ws.rs.{GET => J_GET, POST => J_POST}
import org.tresql._
import org.wabase.Authentication.{BasicAuth, Crypto}
import org.wabase._
import spray.json._
import uniso.app.biz.UserManager

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

  override def initDeferredStorage = new DbDeferredStorage(appConfig, this, App, this)

  override def execution = uniso.app.execution

  override lazy val deferredUris = Set("long-req")
  override lazy val deferredTimeouts = Map("long-req" -> 10.seconds)
  override lazy val deferredWorkerCount = 3

  override val appVersion =
    Option(this.getClass.getResourceAsStream("/version.txt"))
      .map(scala.io.Source.fromInputStream(_).mkString)
      .map(_.trim)
      .filter(_ != "")
      .getOrElse("pagaidu versija " + (new SimpleDateFormat("yyyy.MM.dd HH:mm:ss")).format(new java.util.Date))

  override implicit val userFormat: JsonFormat[user_principal] = UserHelper.userFormat
  lazy val anonymousUser = UserHelper.anonymous

  override val SignedInDefaultPath = "/api/current_user"

  override def remoteAddressToString(ra: RemoteAddress): String = ra.toIP.map(_.ip.getHostAddress).orNull // FIXME to core

  override def signInUser: AuthenticationDirective[user_principal] = {
    val dr: Directive1[Option[user_principal]] = entity(as[JsValue]).flatMap{userInfo =>
      val credentials = userInfo.convertTo[Map[String, Any]](BankQuereaseIo.MapJsonFormat)
      val username = credentials("username").toString
      val password = credentials("password").toString

      onSuccess(UserManager.authenticateUser(username, password))
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

  def login = (path("api" / "login") & post) {
    signIn
  }

  def currentUser = (path("api" / "current_user") & authenticateUser) { u =>
    complete(u)
  }

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
