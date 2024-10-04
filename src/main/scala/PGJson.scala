package uniso.app

import org.postgresql.util.PGobject
import spray.json._

class PGJson extends PGobject{
  setType("json")

  private var json: JsObject = null

  override def setValue(value: String): Unit = {
    json = value.parseJson.asJsObject
    super.setValue(value)
  }

  def setJson(value: JsObject): Unit = {
    json = value
    super.setValue(value.toString)
  }


  def getJson = json
}

object PGJson{
  def apply(json: JsObject) = {
    val result = new PGJson
    result.setJson(json)
    result
  }

  def apply(json: JsValue) = {
    val result = new PGJson
    result.setValue(json.toString)
    result
  }
}
