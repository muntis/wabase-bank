package uniso.app

import org.apache.pekko.http.scaladsl.model.{HttpEntity, HttpHeader, HttpMessage, HttpMethod, HttpMethods, HttpRequest, HttpResponse, MessageEntity}
import org.apache.pekko.http.scaladsl.server.Route
import dto.user_principal
import org.apache.pekko.http.scaladsl.marshalling.Marshaller
import org.apache.pekko.http.scaladsl.unmarshalling.FromResponseUnmarshaller
import org.wabase.*
import org.wabase.AppMetadata.{Action, AugmentedAppFieldDef}
import org.tresql.*
import org.wabase.AppQuerease.InjectionParametersContext
import org.wabase.client.WabaseHttpClient

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object App
  extends AppBase[user_principal]
    with PostgreSqlConstraintMessage
    with BankAuthorization
    with AppFileStreamer[user_principal]
    with AppConfig
    with NoValidation
    with BankAudit[user_principal]
    with DbAccess {

  implicit val defaultCP: PoolName = DEFAULT_CP
  override protected def initQuerease: org.wabase.AppQuerease = org.wabase.DefaultAppQuerease

  println("AppServer.port === " + AppServer.port)
  val httpClient = new WabaseHttpClient{
    override lazy val port = AppServer.port
    override lazy val serverPath = s"http://localhost:$port/"

    // TODO to show examples with service calls should make
    // some services where there is different auth methdod
    override def login(username: String, password: String): String = {
      httpPostAwait[Map[String, Any], String](HttpMethods.POST, "api/login", Map("username" -> username, "password" -> password))
    }
    def isLoginRequest(req: HttpRequest) =
      req.uri.toString.endsWith("api/login") || req.uri.toString.endsWith("api/current_user")

    def serviceCallCookieMap = {
      println("LOGIN")
      login("admin@localhost", "admin")
      println("LOGIN DONE")
      getCookieStorage
    }


    override def doRequest(req: HttpRequest, cookieStorage: CookieMap, timeout: FiniteDuration, maxRedirects: Int): Future[HttpResponse] = {
      println("DO REQUEST ====")
      println(req)
      println(isLoginRequest(req))
      println("DO REQUEST -----")
      super.doRequest(req, if (isLoginRequest(req)) cookieStorage else serviceCallCookieMap, timeout, maxRedirects)
    }
  }

  println("AppServer.httpClient.port === " + httpClient.port)
  println("AppServer.httpClient.serverPath === " + httpClient.serverPath)

  def messageToString(message: HttpMessage): String = {
    val enitty = message.entity match {
      case HttpEntity.Strict(_, data) => " Body: " + data.utf8String
      case _ => " Streamed entity"
    }
    message.toString + "\n" + enitty
  }

  def log(client: HttpRequest => Future[HttpResponse])(inj: InjectionParametersContext)(req: HttpRequest): Future[HttpResponse] = {
    import AppService._
    for{
      request <- Future.successful(req)
      _ = logger.debug("HttpClient request: " + messageToString(request))
      response <- client(req)
      _ = logger.debug("HttpClient response: " + messageToString(response))
    }yield response
  }

  override implicit lazy val httpClients: WabaseHttpClients =
    WabaseHttpClients(Map("default" -> log(httpClient.doRequest)))

  override def toAuditableMap(user: user_principal): Map[String, Any] = Map(
    "id" -> user.id,
    "adm_user_id" -> user.id,
    "pk" -> user.pk,
    "email" -> user.email,
    "name" -> user.name,
    "surname" -> user.surname,
    "ip_address" -> user.ip_address,
    "user_agent" -> user.user_agent,
  )

  override def current_user_param(user: user_principal): Map[String, Any]
  = Option(user).map(u => Map("current_user_id" -> u.id)) getOrElse Map.empty

}

