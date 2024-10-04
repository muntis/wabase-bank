package uniso.app

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.wabase.{Dto, DtoWithId, TemplateUtil}
import org.scalatest.matchers.should.Matchers
import org.tresql.Query
import org.wabase.AppMetadata.AugmentedAppViewDef

class DataSpecs extends AnyFlatSpec with Matchers with RunningServer with BeforeAndAfterAll with TemplateUtil {
  import App._

  val ApplicationStateCookiePrefix = "current_"

  override def resourcePath = "src/it/resources/"

  def defaultListParams = Map("query" -> "", "limit" -> 1)

  private var defaultListParamsForClass: Map[Class[_ <: Dto], Map[String, Any]] = Map(
  )

  case class ParamsForGet(
    forId: Map[String, Any] = Map.empty,
    forObject: Map[String, Any] = Map.empty,
    extraSql: String = null
  )
  private val defaultGetParamsForClass: Map[Class[_ <: Dto], ParamsForGet] = Map(
  )

  val excludeList = Seq(
  )

  val excludeGetList = Seq(
  )

  def views =
    qe
      .collectViews { case v => v }
      .toSeq
      .sortBy(_.name)

  def listTest(clzz: Class[_ <: Dto], params: Map[String, Any]): Unit = defaultListParamsForClass += clzz -> params
  def listTest(clzz: Class[_ <: Dto], name: String, params: Map[String, Any]): Unit = createListTest(clzz, name, false, params)

  views.foreach(view => {
    val viewClass = qe.viewNameToClassMap(view.name)
    view.apiMethodToRoles.foreach({
      case ("list", _) if !excludeList.contains(view.name) =>
        createListTest(viewClass, null, view.apiMethodToRoles.contains("count"), defaultListParamsForClass.getOrElse(viewClass, Map.empty))
      case x => logger.debug(s"Test case not supported yet: ${view.name}.${x._1}")
    })
  })

  override def beforeAll() = {
    login()
    listenToWs(deferredActor)
  }

  private def createListTest(viewClass: Class[_ <: Dto], name: String, shouldCount: Boolean, params: => Map[String, Any]) = {
    it should "return list of "+viewClass+Option(name).map(" with "+_).getOrElse("") in {
      testList(viewClass, params)
    }

    if (shouldCount) {
      it should "return count for list of " + viewClass + Option(name).map(" with " + _).getOrElse("") in {
        testCount(viewClass, params)
      }
    }
  }

  private def createGetTest(viewClass: Class[_ <: Dto], name: String, id: Long, shouldSave: Boolean, params: => Map[String, Any]): Unit = {
    it should s"get${if (shouldSave) "/save" else ""} record of $viewClass ${Option(name).map(" with "+_).getOrElse("")}; id: $id" in {
      testGet(viewClass, id, shouldSave, params)
    }
  }

  def testList(viewClass: Class[_ <: Dto], params: => Map[String, Any]): Unit = {
    login()
    val (cookies, filteredParams) = params.partition(_._1.startsWith(ApplicationStateCookiePrefix))
    getCookieStorage.setCookies(cookies)
    list(viewClass, filteredParams ++ defaultListParams)
    clearCookies
  }

  def testCount(viewClass: Class[_ <: Dto], params: => Map[String, Any]): Unit = {
    login()
    val (cookies, filteredParams) = params.partition(_._1.startsWith(ApplicationStateCookiePrefix))
    getCookieStorage.setCookies(cookies)
    count(viewClass, filteredParams ++ defaultListParams)
    clearCookies
  }

  def testGet(viewClass: Class[_ <: Dto], id: Long, shouldSave: Boolean, params: => Map[String, Any]): Unit = {
    login()
    val (cookies, _) = params.partition(_._1.startsWith(ApplicationStateCookiePrefix))
    getCookieStorage.setCookies(cookies)
    val x = get(viewClass, id, params)
    if (shouldSave) {
      save(x.asInstanceOf[DtoWithId])
    }
    clearCookies
  }
}
