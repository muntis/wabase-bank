package uniso.app

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import spray.json._

class AppServiceBaseSpecs extends AnyFlatSpec with should.Matchers with RunningServer with WithHttpClient{
  final lazy val httpClient = initHttpClient
  import httpClient._

  behavior of "AppServiceBase"

  it should "return api list" in {
    login()
    httpGetAwait[JsValue]("api").asJsObject.fields.foreach{
      case (view, JsArray(api))  => api should not be Symbol("empty")
      case (view, struc) => fail(view+" does not contain array: "+struc)
    }
    clearCookies
  }

  it should "return metadata list" in {
    def assertViewDef(viewName: String, viewDef: JsObject) = {
      viewDef.fields("name") match {
        case JsString(name) => name should be (viewName)
        case struc => fail("View name should be string: "+struc)
      }
    }
    login()
    httpGetAwait[JsValue]("metadata/*").asJsObject.fields.foreach{case (view, definition) =>
      assertViewDef(view, definition.asJsObject)
      val defFromSpecificReq = httpGetAwait[JsValue]("metadata/"+view)
      assertViewDef(view, defFromSpecificReq.asJsObject)
    }
    clearCookies
  }

}
