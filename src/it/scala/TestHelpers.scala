package uniso.app

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.mojoz.querease.ValidationResult
import org.wabase.JsonConverterProvider
import org.wabase.client.{ClientException, RestClient}
import spray.json._

trait TestHelpers {this: RestClient & JsonConverterProvider & org.scalatest.matchers.should.Matchers =>

  import org.wabase.DefaultAppQuereaseIo.MapJsonFormat

  def parseError(t: ClientException): List[ValidationResult] = t.getCause match{
    case null =>
      val json = t.responseContent.parseJson.asJsObject.convertTo[Map[String, Any]]
      json.keys.map(f => {
        ValidationResult(List(f), List(json(f).toString))
      }).toList
    case c: ClientException => parseError(c)
  }

  def expectError(f : => Any) = {
    val error = intercept[ClientException](f)
    error.status shouldBe StatusCodes.BadRequest

    parseError(error)
  }

  def uniqueName(prefix: String) = prefix + "_" + System.currentTimeMillis()

}
