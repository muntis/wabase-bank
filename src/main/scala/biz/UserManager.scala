package uniso.app.biz

import dto.user_principal
import org.mojoz.querease.ValidationException
import uniso.app.App._
import org.wabase.{Authentication, BusinessException, PoolName, config}
import uniso.app._
import org.tresql._
import uniso.app.AppService.app

import scala.concurrent.Future
import scala.util.{Random, Try}

class UserManager {

  def generate_pwd(ctx: Map[String, Any]): String = {
    val random = new java.security.SecureRandom()
    val pwdRequriments =
      List(
        (6, "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ"),
        (2, "abcdefghjkmnpqrstuvwxyz"),
        (2, "0123456789"),
        (1, "!#$*?/()=+-:><")
      )

    val letterList = pwdRequriments.head._2.toList

    def rngStringOf(len: Int, in: List[Char]) = Seq.fill(len)(in(random.nextInt(in.length - 1)))

    val res = pwdRequriments.flatMap{case (numberOfChars, chars) =>
      rngStringOf(numberOfChars, chars.toList)
    }

    s"${rngStringOf(1, letterList).head}${Random.shuffle(res).mkString("")}"
  }


  def check_pwd_history(ctx: Map[String, Any]) = dbUse {
    ctx.get("id").filterNot(_ == null).foreach { case id: Long =>
      ctx.get("parole").filterNot(_ == null).map(String.valueOf).foreach { pwd =>
        tresql"adm_password_history [adm_user_id = $id & passwd != null] {passwd}#(~change_time)@(5)".list[String].foreach { pwdHash =>
          val existsPwd = Try(org.wabase.Authentication.checkPassword(pwd, pwdHash)).getOrElse(false)
          if (existsPwd)
            throw new BusinessException("Parole nedrīkst būt vienāda ar kādu no 5 iepriekš izmantotajām parolēm!")
        }
      }
    }
  }
}

object UserManager {
  def authenticateUser(username: String, password: String, ip: String, userAgent: Option[String]): Future[Option[user_principal]] = {
    import app._
    val user = transaction {
      getPwdHash(username).headOption
        .filter {t => org.wabase.Authentication.checkPassword(password, t.passwd)}
        .flatMap { _ =>
          updateUserOnSuccessfullLogin(username, ip)
          UserHelper.make(username, ip, userAgent)
        }.map { u => App.auditLogin(u, u); u
        }.orElse {
          updateUserOnFailedLogin(username, ip)
          failedLoginAudit("Login failed", Map("username" -> username, "password" -> password), username, ip, userAgent)
          None
        }
    }
    Future.successful(user)
  }

  def failedLoginAudit (errorText: String, credentials: Map[String, Any], username: String, ip: String, userAgent: Option[String]): Unit = {
    val u = new user_principal
    u.name = username
    u.ip_address = ip
    u.user_agent = userAgent.orNull
    App.auditLoginFailure(u, errorText, credentials)
  }

  def updateUserOnSuccessfullLogin(username: String, ip: String) = {
    tresql"=adm_user[email = $username]{failed_login_attempts = 0, last_login_time = now(), last_login_ip = $ip}"
  }

  def updateUserOnFailedLogin(username: String, ip: String) = {
    tresql"""=adm_user[email = $username]{
            failed_login_attempts = failed_login_attempts + 1,
            status = case(failed_login_attempts + 1 >= 3 & status = 'Active', 'Blocked', status),
            last_failed_login_time = now(),
            last_failed_login_success_ip = $ip}"""
  }

  def getPwdHash(username: String) = {
    tresql"adm_user[email = $username & status = 'Active' & exists(adm_user_role[adm_user_id = adm_user.id & date_from <= current_date & current_date <= coalesce(date_to, current_date)])] {id, passwd}"
  }

}

