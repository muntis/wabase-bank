package uniso.app

import org.apache.pekko.http.scaladsl.model.{HttpEntity, HttpMessage, HttpRequest, HttpResponse}
import org.apache.pekko.http.scaladsl.server.Route
import dto.user_principal
import org.wabase.*
import org.wabase.AppMetadata.{Action, AugmentedAppFieldDef}
import org.tresql.*
import org.wabase.AppQuerease.InjectionParametersContext
import org.wabase.client.WabaseHttpClient

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

  val httpClient = new WabaseHttpClient{
    override lazy val port = AppServer.port
  }

  // Set of URIs that should be kept chunked
  val longRequests: Set[String] = Set()
  def toStrictEntity(req: HttpRequest): Future[HttpRequest] = {
    import AppService._
    val doToStrict = req.uri.path.isEmpty || !longRequests(req.uri.path.reverse.head.toString)
    if(doToStrict){
      val entity = req.entity.toStrict(httpClient.requestTimeout)
      entity.map(e => req.withEntity(e))
    }else Future.successful(req)
  }

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
      request <- toStrictEntity(req)
      _ = logger.debug("HttpClient request: " + messageToString(request))
      response <- client(request)
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

