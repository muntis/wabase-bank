package uniso.app

import akka.http.scaladsl.model.HttpMethods
import org.wabase.AppQuerease
import org.wabase.client.WabaseHttpClient
import org.scalatest.matchers.should.Matchers

import scala.language.reflectiveCalls

trait RunningServer extends WabaseHttpClient with Matchers with TestHelpers {

  override protected def initQuerease: AppQuerease = BankQuerease
  override lazy val port = AppServer.port

  override lazy val serverWsPath: String = s"ws://localhost:$port/services/ws"

  override val defaultUsername: String = "admin@localhost"
  override val defaultPassword: String = "admin"


  override def login(username: String = defaultUsername, password: String = defaultPassword) = {
    val response = httpPostAwait[Map[String, Any], String](HttpMethods.POST, "api/login", Map("username" -> username, "password" -> password))
    logger.debug(s"login user $username, password $password, response: $response")
    response
  }

  ServerState.synchronized{
    if(!ServerState.running){
      AppServer.main(Array.empty)
      ServerState.running = true
    }
  }


}

private object ServerState{
  var running = false
}
