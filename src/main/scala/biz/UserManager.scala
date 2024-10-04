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


  def p_new_hash(ctx: Map[String, Any]): String = {
    Authentication.passwordHash(String.valueOf(ctx("p_new")))
  }

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

  // FIXME do not like this
  def authenticateUser(username: String, password: String): Future[Option[user_principal]] = {
    import app._
    val user = transaction {
      getPwdHash(username).headOption
        .filter {t => org.wabase.Authentication.checkPassword(password, t.passwd)}
        .flatMap { _ =>
          resetErrorCounter(username) // moved due to some weird implicit conflict
          //          LietotajiManager.updateSuccesfulSessionInfo(username, ip)
          UserHelper.make(username)
        }.orElse {
          updateErrorCounter(username)
          //         LietotajiManager.updateFailedSessionInfo(username, ip)
          None
        }
    }
    Future.successful(user)
  }

  def checkPassword(ctx: Map[String, Any]) = {
    if (!Authentication.checkPassword(String.valueOf(ctx("p_old")), String.valueOf(ctx("p_old_hash"))))
      throw new BusinessException("Esošā parole nav pareiza.")
  }

  def getPwdHash(username: String) = {
    tresql"adm_user[lower(email) = ${username.toLowerCase} & status = 'Active' & exists(adm_user_role[adm_user_id = adm_user.id & date_from <= current_date & current_date <= coalesce(date_to, current_date)])] {id, passwd}"
  }

  def resetErrorCounter(username: String) = {
    tresql"=adm_user[lower(email) = ${username.toLowerCase}]{failed_login_attempts = 0}"
  }

  def updateErrorCounter(username: String) = {
    tresql"=adm_user[lower(email) = ${username.toLowerCase}]{failed_login_attempts = failed_login_attempts + 1, status = case(failed_login_attempts + 1 >= 3 & status = 'Active', 'Blocked', status)}"
  }

  def updateSuccesfulSessionInfo(username: String, ip: String) = {
    tresql"=adm_user[lower(email) = ${username.toLowerCase}]{last_login_time = now(), last_login_ip = $ip}"
  }

  def updateFailedSessionInfo(username: String, ip: String) = {
    tresql"=adm_user[lower(email) = ${username.toLowerCase}]{last_failed_login_time = now(), last_failed_login_success_ip = $ip}"
  }
}

