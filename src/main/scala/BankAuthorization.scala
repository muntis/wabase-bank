package uniso.app

import dto.user_principal
import org.wabase._


trait BankAuthorization extends Authorization[user_principal] { this: App.type =>

  override def check[C <: RequestContext[?]](ctx: C, clazz: Class[?]) = {}

  override def can[C <: RequestContext[?]](ctx: C, clazz: Class[?]): Boolean = true

  override def relevant[C <: RequestContext[?]](ctx: C, clazz: Class[?]): C = ctx

  override def hasRole(user: user_principal, roles: Set[String]): Boolean = roles.exists(r => hasRole(user.id, r))

  def hasRole(userId: Long, role: String) = dbUse {
    val result = if (role == "PUBLIC") true
    else if (role == "LOGGED_IN_USER" && userId != -1) true // FIXME user logged in ... (maybe use user != Guest)
    else qe.list[dto.has_role_helper](Map("current_user_id" -> userId, "role" -> role))
      .headOption.exists(_.has_role.booleanValue)
    logger.debug(s"hasRole userId: $userId, role: $role, result: $result")
    result
  }

}
