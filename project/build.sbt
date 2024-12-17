resolvers += "sonatype-oss-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

// updateOptions := updateOptions.value.withCachedResolution(true).withLatestSnapshots(false)

libraryDependencies += "org.wabase"  %% "wabase"  % "7.0.0-RC20-SNAPSHOT"

