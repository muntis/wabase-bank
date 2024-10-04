package uniso.app

import App._
import dto.user_principal
import org.tresql._
import org.wabase.jLong
import spray.json.DefaultJsonProtocol._
import spray.json._
import uniso.app.AppService.dtoJsonFormat


object UserHelper {
  def make(email: String): Option[user_principal] = dbUse {
    App.qe.list[user_principal](Map("email" -> email)).headOption.map { user =>
      App.auditLogin(user, user)
      user
    }
  }

  val userFormat: JsonFormat[user_principal] = dtoJsonFormat[user_principal]

  lazy val anonymous : user_principal = dbUse {
    //val user_id = tresql"adm_lietotajs[epasts='anonymous']{ id }".unique[Long]
    val user = new user_principal
    user.id = -1  // FIXME
    user.name = "anonymous"
    user
  }
}
