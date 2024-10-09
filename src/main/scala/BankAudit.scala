package uniso.app

import dto.user_principal
import org.tresql.{InsertResult, ORT}
import org.wabase.MapRecursiveExtensions._
import org.wabase.Audit.AbstractAudit
import org.wabase._
import spray.json._

import scala.language.reflectiveCalls
import scala.util.{Success, Try}

trait BankAudit[user_principal] extends AbstractAudit[user_principal] {
  this: AppBase[user_principal] & DbAccess & Authorization[user_principal] & ValidationEngine & DbConstraintMessage =>
  import org.wabase.DefaultAppQuereaseIo._

  val nodeName = java.net.InetAddress.getLocalHost.getHostName
  implicit val extraDb: Seq[org.wabase.AppMetadata.DbAccessKey] = Nil

  //def auditSource: String

  override def logUnchangedSaves = true

  val auditEnabled = config.hasPath("audit.enabled") && config.getBoolean("audit.enabled")
  val auditPool = if (config.hasPath("audit.pool")) config.getString("audit.pool") else "audit"
  val poolName = PoolName(auditPool)
  // Prestart check
  if (auditEnabled && !config.hasPath(s"jdbc.cp.$auditPool"))
    sys.error(s"""Audit is enabled but there is no connection pool configured for "jdbc.cp.${auditPool}"! Maybe need to set different pool name for audit (key "audit.pool", default "audit")?""")

  def auditLoginFailure(user: user_principal, error: String, params: Map[String, Any]) = {
    audit(AuditData(action = "login", user = user, newData = removeBlacklistedFields(params), time = now, error = error))
  }

  def auditLogout(user: user_principal): Unit = {
    audit(AuditData(action = "logout", user = user, time = now))
  }

  override val blackListedFields = Set("auth", "password", "repeated_password", "passwd", "kļūdas", "parole", "p_old", "p_new", "p_repeat")

  def toAuditableMap(user: user_principal): Map[String, Any]

  override def audit(data: AuditData): Unit = {
    import data._
    val userData = toAuditableMap(user)
    val userDataNoId = userData - "id"

    action match {
      case "list" | "count" =>
      case "login" => log(Map("action" -> "login", "user_data" -> userData, "error_data" -> error, "json_data" -> Map[String, Any]("new_data" -> newData).toJson) ++ userDataNoId, data.relevant_id)
      case _ => if(action != "view" || entity == "lietotajs_editable") 
        log(Map(
          "action" -> action,
          "source" -> "PORTAL", // TODO temporary source
          "entity_id" -> entity_id,
          "entity" -> entity,
          "error_data" -> error,
          "json_data" -> Map[String, Any](
            "new_data" -> (if (newData != null) removeBlacklistedFields(newData) else newData),
            "user_data" -> userData,
            "old_data" -> (if (oldData != null) removeBlacklistedFields(oldData) else oldData),
            //"diff" -> diff,
          ).toJson
        ) ++ userDataNoId, data.relevant_id)
    }
  }

  override def audit(context: AppActionContext, result: Try[QuereaseResult]): Unit = {
    val idOption = context.values.get("id").filter(_ != null).map(_.asInstanceOf[jLong]).orElse(
      result match{
        case Success(IdResult(null, _)) => None
        case Success(IdResult(id, _)) => Some(id.asInstanceOf[jLong])
        //case Success(OptionResult(op: Option[DtoWithId] @unchecked)) => op.map(_.id)
        case _ => None
      }
    )

    val data = AuditData(
      action = context.actionName match{ // FIXME when wabase stops fluctuating, standartize this
        case "save" | "insert" if context.oldValue == null => "create"
        case "update" => "save"
        case "delete" => "remove"
        case "get" => "view"
        case rest => rest
      },
      entity_id = idOption.orNull,
      user = context.user, // context.,
      time = now,
      entity = context.viewName,
      newData = context.values,
      oldData = context.oldValue,
      error = result.failed.toOption.map(_.getMessage).orNull
    )
    audit(data)
  }


  override def relevantKeys(view: String) = Set("id", "adm_lietotajs_id")

  def log(data: Map[String, Any], relevantIds: List[Long]) = {
    if (auditEnabled) {
      implicit val poolName = PoolName(auditPool)
      val auditData = (data ++ Map(
        "request_time" -> now,
        "node_name" -> nodeName,
        "status" -> (if (data.get("error_data").exists(_ != null)) "error" else "success")
      )).recursiveMap{
        case (_, jso: JsObject) => PGJson(jso)
      }
      val id = transactionNew {
        val Some(id: Long) = ORT.insert("audit", auditData).asInstanceOf[InsertResult].id: @unchecked
        //if (relevantIds != null) relevantIds.foreach{relevantId => ORT.insert("audit_data_relevant_ids", Map("audit_data_id" -> id, "relevant_id" -> relevantId))}
        id
      }
    } else {
      logger.warn(s"Trying to audit some action, but audit is disabled: $data")
    }
  }
}
