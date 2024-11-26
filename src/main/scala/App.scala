package uniso.app

import dto.user_principal
import org.wabase.*
import org.wabase.AppMetadata.{Action, AugmentedAppFieldDef, KnownFieldExtras, KnownViewExtras}
import org.tresql.*

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
  protected def initQuerease: org.wabase.AppQuerease = new org.wabase.AppQuerease{
    override lazy val knownFieldExtras = KnownFieldExtras() + "can_copy"
    override lazy val knownViewExtras = KnownViewExtras() + "can_copy"
  }

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

  // Lists all views that can be copied with all fields that can be copied
  App.qe.nameToViewDef.values.filter(_.extras.getOrElse("can_copy", false).asInstanceOf[Boolean]).foreach { v =>
    logger.debug(s"View ${v.name} can be copied, with following fields: ")
    v.fields.filter(_.extras.getOrElse("can_copy", false).asInstanceOf[Boolean]).foreach { f =>
      logger.debug(s"Field ${f.name} can be copied")
    }
  }
}

