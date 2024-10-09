package uniso.app

import akka.http.scaladsl.model.HttpCharsets.`UTF-8`
import akka.http.scaladsl.model.MediaTypes.`text/html`
import akka.http.scaladsl.model.{ContentType, MediaTypes}
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.pathPrefix
import com.github.swagger.akka.SwaggerHttpService.readerConfig
import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import org.mojoz.metadata.{FieldDef, ViewDef}
import io.swagger.v3.jaxrs2.Reader
import io.swagger.v3.oas.models.{Components, OpenAPI, Operation, PathItem, Paths}
import io.swagger.v3.oas.models.media.{ArraySchema, BooleanSchema, Content, DateSchema, DateTimeSchema, FileSchema, IntegerSchema, JsonSchema, MediaType, NumberSchema, ObjectSchema, Schema, StringSchema}
import io.swagger.v3.oas.models.parameters.{PathParameter, QueryParameter, RequestBody}
import io.swagger.v3.oas.models.responses.{ApiResponse, ApiResponses}
import jdk.jfr.Description
import org.mojoz.querease.FilterType.{ComparisonFilter, OtherFilter}
import org.wabase.{AppBase, AppFileServiceBase, config}
import org.wabase.AppMetadata.{AugmentedAppFieldDef, AugmentedAppViewDef}
import org.wabase.AppBase.FilterParameter
import org.wabase.AppMetadata.Action.{Evaluation, Validations, ViewCall}

import java.net.URI
import scala.language.existentials
import scala.jdk.CollectionConverters._
import java.util
import scala.io.Source

// Only for annotations use
class faila_augšupielādes_forma {
  @io.swagger.v3.oas.annotations.media.Schema(`type` = "string", format = "binary")
  val file: String = ""
}

class faila_izmēra_ierobežojums {
  @io.swagger.v3.oas.annotations.media.Schema(description = "Saņemtais faila izmērs baitos")
  val actualSize: Long = 0
  @io.swagger.v3.oas.annotations.media.Schema(description = "Maksimālais faila izmērs baitos")
  val limit: Long = 0
}

object SwaggerDocService extends SwaggerHttpService{
  val hostString = {
    if (config.hasPath("app.host")) {
      config.getString("app.host").stripSuffix("/")
    } else "http://localhost:8090"
  }
  val appHost = new URI(hostString)
  override val host =  appHost.getHost + (if (appHost.getPort == -1 ) "" else  ":" + appHost.getPort) //the url of your api, not swagger's json endpoint
  override val schemes =  List(appHost.getScheme) //the url of your api, not swagger's json endpoint


  override val apiClasses: Set[Class[?]] = Set(AppService.getClass)

  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info(version = AppService.appVersion) //provides license and other description details
  override val unwantedDefinitions = List("Nothing$", "Function1AppQuereaseFunction1StringPartialFunctionObjectListTuple2StringObject", "Function1RequestContextFutureRouteResult")


  def respondWithMediaType(contentType: ContentType): Directive0 =
    mapResponseEntity(_ withContentType contentType)

  def loadFile(path: String) = {
    val source = Source.fromURL(getClass().getResource(path))
    val result = source.mkString
    source.close()
    result
  }

  val swaggerUiInitializerFile = {
    val templateParams = Map("url" -> (hostString + "/api-docs/swagger.json"))
    def applyTemplate(template: String) =
      templateParams.foldLeft(template) { case (t, (key, value)) => t.replace(s"{$key}", value) }
    applyTemplate(loadFile("/ui/swagger-initializer.js"))
  }
  val respondWithSwaggerUiInitializerFile = respondWithMediaType(ContentType(MediaTypes.`application/javascript`, `UTF-8`)) {
    complete {
      swaggerUiInitializerFile
    }
  }

  def systemGeneratedFilterParams = Set("current_user_id")

  val viewdefs = dropOrphans(org.wabase.DefaultAppQuerease.nameToViewDef.values.toList)
  val viewDefMap = viewdefs.map(v => v.name -> v).toMap

  // https://swagger.io/docs/specification/data-models/data-types/
  def schemaFromtype(type_ : org.mojoz.metadata.Type) = type_.name match{
    case n if type_.isComplexType => (new ObjectSchema).$ref(refFromViewName(n))
    case "long" => (new IntegerSchema).format("int64")
    case "int" => new IntegerSchema
    case "decimal" => new NumberSchema
    case "boolean" => new BooleanSchema
    case "date" => new DateSchema
    case "dateTime" => new DateTimeSchema
    case "string" =>
      val s = new StringSchema
      type_.length.foreach(l => s.maxLength(l))
      s
    case "json" => new JsonSchema
    case n => (new ObjectSchema).`type`(n)
  }

  def getReadOnly(viewdefs: Map[String, ViewDef])(field: FieldDef) = {
    if (!field.api.updatable && !field.api.insertable) true
    else if (field.type_.isComplexType) viewdefs.get(field.type_.name).exists(_.saveTo == Nil)
    else false
  }

  def fieldRequired(viewdefs: Map[String, ViewDef])(field: FieldDef) = !getReadOnly(viewdefs)(field) && (field.required || !field.nullable)

  def addEnumIfNeeded(enums: Seq[String], schema: Schema[?]) = {
    (enums, schema) match{
      case (enums, stringSchema: StringSchema) if enums != null => enums.foreach(stringSchema.addEnumItem)
      case _ =>
    }
    schema
  }

  def schemaFromFieldDef(viewdefs: Map[String, ViewDef])(field: FieldDef) = {
    val maybeArraySchema = if (field.isCollection) {
      new ArraySchema().items(schemaFromtype(field.type_))
    }else schemaFromtype(field.type_)
    addEnumIfNeeded(field.enum_, maybeArraySchema)

/*    println("-----------------------------------------")
    println("field.name: "+field.name)
    println("field.alias: "+field.alias)
    println("field.saveTo: "+field.saveTo)
    println("field.type_: "+field.type_)
    println("-----------------------------------------")*/

    val fieldName = field.fieldName
    fieldName -> maybeArraySchema
      .name(fieldName)
      .readOnly(getReadOnly(viewdefs)(field))
      .description(Option(field.label).getOrElse(field.comments))
  }

  def schemasFromViewDefs = {
    viewDefMap.view.mapValues {value =>
/*      println("=========================================")
      println("value.name: "+value.name)
      println("value.saveTo: "+value.saveTo)*/

      val filteredFields = value.fields.filterNot(_.api.excluded)
      val fields = filteredFields.map(schemaFromFieldDef(viewDefMap)).toMap

      val requiredFields = filteredFields.filter(fieldRequired(viewDefMap)).map(_.fieldName).toList
      /*    println("=========================================")
          fields.foreach(println)
          println("=========================================")*/
      new ObjectSchema().name(value.name).description(value.comments).properties(fields.asJava).required(requiredFields.asJava)
    }.toMap
  }

  def refFromViewName(viewName: String) = s"#/components/schemas/$viewName"

  def jsonContent(view: String, array: Boolean = false): Content = {
    val content = new Content
    val mediaType = new MediaType
    val viewSchema = (new Schema()).$ref(refFromViewName(view))
    val maybeArraySchema = if (array) {
      new ArraySchema().items(viewSchema)
    }else viewSchema

    mediaType.setSchema(maybeArraySchema)
    content.addMediaType("application/json", mediaType)

    content
  }

  def addExcelContent(content: Content): Unit = {
    def addContext(mType: String)= {
      val mediaType = new MediaType
      val schema = new FileSchema
      schema.setType(mType)
      mediaType.setSchema(schema)
      content.addMediaType(mType, mediaType)
    }
    addContext("application/vnd.ms-excel")
    addContext("application/vnd.oasis.opendocument.spreadsheet")
  }

  def createOperation(summary: String) = (new Operation).summary(summary)
  def validationResultViewName = "kļūdu_saraksts"

  implicit class OperationExtras(val op: Operation){
    def withIdInPath: Operation = {
      op.addParametersItem{
        val p = new PathParameter
        p.name("id")
        p.setSchema((new IntegerSchema).format("int64"))
        p
      }
      op
    }

    def addQueryParams(params: Seq[FilterParameter]): Operation = {
      if(params != null){
        params.filter(p => !systemGeneratedFilterParams(p.name)).foreach{ param =>
          val p = new QueryParameter
          p.name(param.name)
          val required = param.filterType match{
            case OtherFilter(filter) if filter.startsWith("if_defined") => false
            case _ => param.required
          }
          val schema = param.filterType match{
            case ComparisonFilter(_, "in", _, _) => new ArraySchema().items(schemaFromtype(param.type_))
            // TODO should have better filter processing. This one checks for complex exists queries. if there is "in" statement with given param
            case OtherFilter(text) if text.contains(s" in :${param.name}") => new ArraySchema().items(schemaFromtype(param.type_))
            case _ => schemaFromtype(param.type_)
          }

          p.required(required)
          p.setSchema(addEnumIfNeeded(param.enum_, schema))
          op.addParametersItem(p)
        }
      }
      op
    }

    def addListAndSortParams(sortOptions: Seq[String]): Operation = {
      ((if(sortOptions == null || sortOptions.isEmpty) Nil else
        List(
          ("sort",
            "Lauks pēc kura kārtot, ~ lauka priekšā apzīmē dilstošu kārtošanu",
            addEnumIfNeeded(sortOptions.flatMap(v => List(v, "~"+v)), new StringSchema))
        ))
      ++ List(
        ("offset", "Pirmā ieraksta indeks", new IntegerSchema),
        ("limit", "Atgrirežamo ierakstu skaits", new IntegerSchema),
      )).foreach{pp =>
        val p = new QueryParameter
        p.name(pp._1)
        p.description(pp._2)
        p.setSchema(pp._3)
        op.addParametersItem(p)
      }
      op
    }

    def getResponses = if (op.getResponses == null) {
      val r = new ApiResponses
      op.setResponses(r)
      r
    } else op.getResponses

    def addIntegerResponse(description: String, example: String, code: String = "200"): Operation = {
      val responses = getResponses
      val response = new ApiResponse
      response.description(description)

      val content = new Content
      val mediaType = new MediaType
      mediaType.setSchema((new IntegerSchema).example(example))
      content.addMediaType("text/plain", mediaType)
      response.content(content)
      responses.addApiResponse(code, response)
      op
    }

    def addSuccessResponse(view: String, code: String = "200", array: Boolean = false): Operation = {
      val responses = getResponses
      val response = new ApiResponse
      val content = jsonContent(view, array)
      if(array) addExcelContent(content)
      response.content(content)
      responses.addApiResponse(code, response)
      op
    }

    def addErrorResponse(code: String, description: String, content: Content = null): Operation = {
      val responses = getResponses
      val response = new ApiResponse
      response.description(description)
      if(content != null) response.content(content)
      responses.addApiResponse(code, response)
      op
    }

    def addNotFoundResponse = addErrorResponse("404", "Ieraksts nav atrasts")
    def addBadRequestResponse(validations: Seq[String] = null) = addErrorResponse("400",
      "Pieprasījumu nav iespējams izpildīt, saņemtie dati nav korekti. "+
        Option(validations).filter(_.nonEmpty).map(v =>"Validāciju kļudu kodi: "+validations.mkString(", ")).getOrElse(""),
//      jsonContent(validationResultViewName, true)
    )
    def addInternalServerError = addErrorResponse("500", "Internal server error")

    def addRequestBody(view: String): Operation = {
      val body = new RequestBody()
      body.setContent(jsonContent(view))
      op.setRequestBody(body)
      op
    }
  }

  def getErrorCodesForView(viewDef: ViewDef, action: String): List[String] = {
    val steps = viewDef.actions.get(action).map(_.steps).getOrElse(Nil)
    val res = steps.flatMap{
      case Validations(Some(name), _, _) => List(name)
      case Evaluation(_, _, ViewCall(method, view, _)) if view != viewDef.name =>
        org.wabase.DefaultAppQuerease.nameToViewDef.get(view).map(subView =>
          getErrorCodesForView(subView, method)
        ).getOrElse(Nil)
      case _ => Nil
    }
    res
  }

  def pathsFromViewDefs: List[(String, PathItem)] = viewdefs.flatMap{ viewDef =>
    val path = "/data/" + viewDef.name
    val countPath = "/count/" + viewDef.name
    val pathWithId = path + "/{id}"
    lazy val filterFields = App.viewNameToFilterMetadata(viewDef.name)
    val ungroupedOperations = viewDef.apiMethodToRoles.toList.flatMap{
      case ("get", apiMethod) =>
        List( (pathWithId, "GET",
          createOperation(s"Atgriezt pēc id '${viewDef.comments}''")
            .withIdInPath
            .addSuccessResponse(view = viewDef.name)
            .addNotFoundResponse
            .addInternalServerError
        ))
      case ("save", apiMethod) if viewDef.fields.exists(_.fieldName == "id") =>
        val validations = getErrorCodesForView(viewDef, "save")
        List(
          ( pathWithId, "PUT",
            createOperation(s"Labot '${viewDef.comments}''")
              .withIdInPath
              .addSuccessResponse(view = viewDef.name)
              .addRequestBody(view = viewDef.name)
              .addBadRequestResponse(validations)
              .addNotFoundResponse
              .addInternalServerError
          ),
          ( path, "POST",
            createOperation(s"Pievienot '${viewDef.comments}''")
              .addSuccessResponse(view = viewDef.name)
              .addRequestBody(view = viewDef.name)
              .addBadRequestResponse(validations)
              .addInternalServerError
          )
        )
      case ("update", apiMethod) if viewDef.fields.exists(_.fieldName == "id") =>
        val validations = getErrorCodesForView(viewDef, "update")
        List(
          (pathWithId, "PUT",
            createOperation(s"Labot '${viewDef.comments}''")
              .withIdInPath
              .addSuccessResponse(view = viewDef.name)
              .addRequestBody(view = viewDef.name)
              .addBadRequestResponse(validations)
              .addNotFoundResponse
              .addInternalServerError
          )
        )
      case ("save", apiMethod) =>
        val validations = getErrorCodesForView(viewDef, "save")
        List(
          ( path, "POST",
            createOperation(s"Pievienot '${viewDef.comments}''")
              .addSuccessResponse(view = viewDef.name)
              .addRequestBody(view = viewDef.name)
              .addBadRequestResponse(validations)
              .addInternalServerError
          )
        )
      case ("insert", apiMethod) =>
        val validations = getErrorCodesForView(viewDef, "save")
        List(
          (path, "POST",
            createOperation(s"Pievienot '${viewDef.comments}''")
              .addSuccessResponse(view = viewDef.name)
              .addRequestBody(view = viewDef.name)
              .addBadRequestResponse(validations)
              .addInternalServerError
          )
        )
      case ("delete", apiMethod) =>
        List((pathWithId, "DELETE",
          createOperation(s"Dzēst '${viewDef.comments}''")
            .withIdInPath
            .addNotFoundResponse
            .addInternalServerError
        ))
      case ("list", apiMethod) =>
        List(( path, "GET",
          createOperation(s"Ierakstu saraksts '${viewDef.comments}''")
            .addSuccessResponse(view = viewDef.name, array = true)
            .addQueryParams(filterFields)
            .addInternalServerError
            .addListAndSortParams(viewDef.fields.filter(_.sortable).map(_.fieldName))
        ))
      case ("count", apiMethod) =>
        List(( countPath, "GET",
          createOperation(s"Ierakstu skaits '${viewDef.comments}''")
            .addIntegerResponse("Ierakstu skaits", "555")
            .addQueryParams(filterFields)
            .addInternalServerError
        ))
    }
    ungroupedOperations.groupBy(_._1).map{case (key, listOfOperations) =>
      val pi = new PathItem
      listOfOperations.foreach{case (_, operationKey, operation) =>
        operationKey match{
          case "GET" => pi.setGet(operation)
          case "PUT" => pi.setPut(operation)
          case "POST" => pi.setPost(operation)
          case "DELETE" => pi.setDelete(operation)
        }
      }
      key -> pi
    }
  }.sortBy(_._1)

  override def reader = new Reader(readerConfig.openAPI(swaggerConfig)){
    override def read(classes: util.Set[Class[?]]): OpenAPI = {
      val api = super.read(classes)
      api.getComponents.getSchemas.asScala.foreach {case (name, schema) =>
        if(schema.getProperties != null)
          schema.setProperties(schema.getProperties.asScala.filter{case (_, ss) => ss == null || ss.get$ref == null || !unwantedDefinitions.exists(ud => ss.get$ref.endsWith("/" + ud))}.asJava)

        org.wabase.DefaultAppQuerease.nameToViewDef.get(name) match{
          case Some(metadata) if schema.getProperties != null =>
            schema.setRequired(metadata.fields.filter(fieldRequired(viewDefMap)).map(_.fieldName).asJava)
          case _ =>
        }

        if (schema.getRequired != null && schema.getProperties != null) {
          val propSet = schema.getProperties.asScala.keySet
          schema.setRequired(schema.getRequired.asScala.filter(propSet).asJava)
        }
      }
      api
    }
  }

  def dropOrphans(views: List[ViewDef]): List[ViewDef] = {
    val viewNamesWithApi = views.filter(view => view.apiMethodToRoles.nonEmpty || view.name == validationResultViewName).map(_.name).toSet
    val viewNamesInFields = views.flatMap(_.fields).filter(_.type_.isComplexType).map(_.type_.name).toSet
    val viewsToReturn = viewNamesWithApi ++ viewNamesInFields
    views.filter(v => viewsToReturn(v.name))
  }

  override def swaggerConfig = {
    val openapi = super.swaggerConfig
    val paths = if (openapi.getPaths == null) {
      val p = new Paths
      openapi.setPaths(p)
      p
    }else openapi.getPaths
    pathsFromViewDefs.foreach{i =>
      paths.addPathItem(i._1, i._2)
    }
    val components =  if (openapi.getComponents == null) {
      val p = new Components
      openapi.setComponents(p)
      p
    }else openapi.getComponents

    schemasFromViewDefs.foreach { i =>
      components.addSchemas(i._1, i._2)
    }
    openapi
  }

  override def routes =
    super.routes ~ pathPrefix("api-docs") {path( "ui" / "swagger-initializer.js"){respondWithSwaggerUiInitializerFile
    }~ path("ui" / Remaining) { resource =>
      getFromResource("ui/" + resource)
    }}
}
