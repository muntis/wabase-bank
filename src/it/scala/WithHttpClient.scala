package uniso.app

import org.wabase.{AppQuerease, DefaultAppQuerease, JsonConverter}
import org.wabase.client.{WabaseHttpClient, HttpClientConfig}
import org.apache.pekko.http.scaladsl.model.HttpMethods

trait WithHttpClient {
  def initHttpClient: WabaseHttpClient = new WabaseHttpClient(HttpClientConfig("test")) {
    override protected def initQuerease: AppQuerease = qe

    override protected def initJsonConverter: JsonConverter[?] = App.qio

    override def login(username: String, password: String) = {
      val response = httpPostAwait[Map[String, Any], String](HttpMethods.POST, "api/login", Map("username" -> username, "password" -> password))
      logger.debug(s"login user $username, password $password, response: $response")
      response
    }
  }

}
