package uniso.app

import org.apache.pekko.http.scaladsl.model.HttpMethods
import org.wabase.{AppQuerease, DefaultAppQuerease, JsonConverter}
import org.wabase.client.{WabaseHttpClient, HttpClientConfig}
import org.scalatest.matchers.should.Matchers

import scala.language.reflectiveCalls

trait RunningServer extends Matchers {


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
