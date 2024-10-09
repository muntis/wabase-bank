package uniso.app

import dto.user_principal
import org.wabase._
import org.wabase.AppMetadata.{Action, AugmentedAppFieldDef}
import org.tresql._

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

  override def toAuditableMap(user: user_principal): Map[String, Any] = Map(
    "id" -> user.id,
    "adm_user_id" -> user.id,
    "pk" -> user.pk,
    "email" -> user.email,
    "name" -> user.name,
    "surname" -> user.surname,
  )

  override def current_user_param(user: user_principal): Map[String, Any]
  = Option(user).map(u => Map("current_user_id" -> u.id)) getOrElse Map.empty

  def failedLoginAudit (errorText: String, credentials: Map[String, Any], username: String, ip: String, userAgent: Option[String], user: user_principal = null): Unit = {
    val u =
      if (user == null) new user_principal
      else user

    u.name = username
    App.auditLoginFailure(u, errorText, credentials)
  }

}

