package uniso.app

import org.wabase.BusinessScenariosBaseSpecs

import scala.util.Random

class BusinessScenariosSpecs extends BusinessScenariosBaseSpecs("business") with RunningServer with WithHttpClient{
  override def initHttpClient: org.wabase.client.WabaseHttpClient = super[WithHttpClient].initHttpClient

  val RandomDecimalNumberStringPattern = "randomDecimalNumberString\\((\\d*)\\)".r
  val RandomEmailPattern = "randomEmail"
  def bankFunctions: Map[String, Any] => PartialFunction[String, Any] = context => {
    case RandomDecimalNumberStringPattern(length) => (1 to length.toInt).map(_ => ('0' + Random.nextInt(10)).toChar).mkString
    case RandomEmailPattern => Random.alphanumeric.take(20).mkString + "@example.com"
  }

  override def templateFunctions: Map[String, Any] => PartialFunction[String, Any] =
    context => bankFunctions(context).orElse(super.templateFunctions(context))


  override lazy val isFullCompareByDefault: Boolean = false
}
