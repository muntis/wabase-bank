package uniso.app

import org.apache.pekko.http.scaladsl.model.HttpRequest
import org.apache.pekko.http.scaladsl.server.Route
import dto.user_principal
import org.wabase.*
import org.wabase.AppMetadata.{Action, AugmentedAppFieldDef}
import org.tresql.*
import org.wabase.client.WabaseHttpClient

import scala.concurrent.ExecutionContext

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
  protected def initQuerease: org.wabase.AppQuerease = org.wabase.DefaultAppQuerease

  val httpClient = new WabaseHttpClient{
    override lazy val port = AppServer.port
  }
  override implicit lazy val httpClients: WabaseHttpClients =
    WabaseHttpClients(Map("default" -> {inj => req => logger.debug("REQ: "+req); httpClient.doRequest(req: HttpRequest)}))

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

