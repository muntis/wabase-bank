resolvers += "sonatype-oss-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

// updateOptions := updateOptions.value.withCachedResolution(true).withLatestSnapshots(false)

libraryDependencies += "org.wabase"  %% "wabase"  % "6.1.1"

unmanagedSources / includeFilter := "FunctionsSignatures.scala"

Compile / unmanagedSourceDirectories += baseDirectory(_ / ".." / "src"  ).value

scalaVersion := "2.12.17"
