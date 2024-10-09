val akkaHttpV = "10.2.10"
val akkaV = "2.6.21"
val wabaseVersion = "7.0.0-RC12-SNAPSHOT"
val jacksonVersion = "2.14.2"
val swaggerVersion = "2.2.10"
//val alpakkaVersion = "4.0.0"  // Ensure Apache-2.0 license

name := "Wabase Sample Bank"
version := "0.1"
scalaVersion := "3.5.1"


libraryDependencies ++= Seq(
    "org.simplejavamail"          % "simple-java-mail"                  % "8.1.2",
    "org.bouncycastle"            % "bcprov-jdk15on"                    % "1.70",
    "org.bouncycastle"            % "bcpg-jdk15on"                      % "1.70",
    "org.bouncycastle"            % "bcpkix-jdk15on"                    % "1.70",
    "org.wabase"                  %% "wabase"                           % wabaseVersion,
    "org.wabase"                  %% "wabase"                           % wabaseVersion % "test" classifier "tests",
    "org.wabase"                  %% "wabase"                           % wabaseVersion % "it" classifier "tests",
 //   "com.typesafe.akka"           %% "akka-http-xml"                    % akkaHttpV,
 //   "com.lightbend.akka"          %% "akka-stream-alpakka-s3"           % alpakkaVersion,
    "io.pebbletemplates"          % "pebble"                            % "3.2.0",
    "org.xhtmlrenderer"           % "flying-saucer-pdf"                 % "9.1.22" excludeAll (
    ExclusionRule(organization = "org.bouncycastle")
    ),
   /* "com.enragedginger"           %% "akka-quartz-scheduler"            % "1.8.2-akka-2.6.x" excludeAll (
    ExclusionRule(organization = "com.zaxxer", name = "HikariCP-java6")
    ),*/
    "ch.qos.logback"              % "logback-classic"                   %  "1.4.7",
    "com.typesafe.akka"           %% "akka-testkit"                     % akkaV % "it,test" cross CrossVersion.for3Use2_13,
    "com.typesafe.akka"           %% "akka-http-testkit"                % akkaHttpV % "it,test" cross CrossVersion.for3Use2_13,
    "org.scalatest"               %% "scalatest"                        % "3.2.11" % "it,test",
    "org.pegdown"                 % "pegdown"                           % "1.6.0" % "it,test",
    "org.apache.poi"              % "poi"                               % "4.1.2",
    "org.apache.poi"              % "poi-ooxml"                         % "4.1.2",
)

libraryDependencies ++= Seq(
    "jakarta.ws.rs"                % "jakarta.ws.rs-api"           % "3.1.0",
    "com.github.swagger-akka-http" %% "swagger-akka-http"          % "2.10.0" cross CrossVersion.for3Use2_13, // FIXME move to scala 3
    "com.github.swagger-akka-http" %% "swagger-scala-module"       % "2.9.0" cross CrossVersion.for3Use2_13,
    "com.github.swagger-akka-http" %% "swagger-enumeratum-module"  % "2.6.1" cross CrossVersion.for3Use2_13,
   // "com.fasterxml.jackson.module" %% "jackson-module-scala"       % jacksonVersion,
    "io.swagger.core.v3"           % "swagger-jaxrs2-jakarta"      % swaggerVersion
)


Compile / unmanagedClasspath += // add function signatures for tresql compiler
  (LocalRootProject / baseDirectory).value / "project" / "target" / "scala-2.12" / "sbt-1.0" / "classes"
Test / unmanagedClasspath += // add function signatures for tresql compiler
  (LocalRootProject / baseDirectory).value / "project" / "target" / "scala-2.12" / "sbt-1.0" / "classes"

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8",
  "-Xmacro-settings:metadataFactoryClass=org.mojoz.querease.TresqlMetadataFactory" +
  ", tableMetadataFile=" + mojozGenerateTresqlTableMetadata.value.getCanonicalPath +
  ", functions=uniso.app.FunctionSignatures",
  "-rewrite",
  "-source:3.4-migration"
)

javaOptions ++= Seq("-Dcom.sun.xml.ws.transport.http.client.HttpTransportPipe.dump=true")
updateOptions := updateOptions.value.withCachedResolution(true).withLatestSnapshots(false)

resolvers += "sonatype-oss-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

enablePlugins(MojozPlugin, MojozGenerateSchemaPlugin, MojozTableMetadataPlugin)

configs(IntegrationTest)

Defaults.itSettings

mojozDtosPackage := "dto"
mojozDtosImports := Seq(
  "org.tresql._",
  "org.wabase.{ Dto, DtoWithId }",
  "uniso.app.App.{ qe, tresqlResources }"
)
mojozSchemaSqlFiles := List(file("db/00-initial/01-schema.sql"))
mojozSchemaSqlGenerators := List(org.mojoz.metadata.out.DdlGenerator.postgresql(typeDefs = mojozTypeDefs.value))
mojozShouldCompileViews := true

Compile / unmanagedResourceDirectories += (LocalRootProject / baseDirectory).value / "swagger-ui"

console / initialCommands := s"""
  """.stripMargin

assembly / assemblyMergeStrategy := {
  case x =>
    // transform all "deduplicate" to "first"
    // Since sbt 1.5.0 `in` is deprecated; migrate to slash syntax
    // https://www.scala-sbt.org/1.x/docs/Migrating-from-sbt-013x.html#slash
    val s = (assembly / assemblyMergeStrategy).value(x)
    if (s == MergeStrategy.deduplicate) MergeStrategy.first else s
}

reStart / mainClass := Some("uniso.app.AppServer")


Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-o", "-h", "report")

//IntegrationTest / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-h", "it-report")

Revolver.enableDebugging(port = 5050, suspend = false)
