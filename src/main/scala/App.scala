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
    with BankDbAccess {

  implicit val defaultCP = DEFAULT_CP
  protected def initQuerease: org.wabase.AppQuerease = BankQuerease

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


  override def validateFields(viewName: String, instance: Map[String, Any])(implicit state: ApplicationState): Unit = {
    val viewDef = qe.viewDef(viewName)
    // TODO ensure field ordering
    val errorMessages = viewDef.fields
      .filterNot(_.api.readonly)
      .map(fld =>
        validationErrorMessage(viewName, fld, instance.getOrElse(fld.fieldName, null))(state.locale))
      .filter(_.isDefined)
      .map(_.get)
      .filter(_ != null)
    if (errorMessages.nonEmpty)
      throw new BusinessException(errorMessages.mkString("\n"))

    // TODO merge all errorMessages?
    val complexFields = viewDef.fields.filter(_.type_.isComplexType).map(fld => fld.fieldName -> fld.type_.name)
    complexFields.foreach { case (fieldName, typeName) =>
      instance.getOrElse(fieldName, null) match {
        case m: Map[String, Any]@unchecked => validateFields(typeName, m)
        case l: Seq[Map[String, Any]]@unchecked => l.foreach(validateFields(typeName, _))
        case null =>
      }
    }
  }



}

