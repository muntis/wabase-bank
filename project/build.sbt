resolvers += "sonatype-oss-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

// updateOptions := updateOptions.value.withCachedResolution(true).withLatestSnapshots(false)

libraryDependencies += "org.wabase"  %% "wabase"  % "8.0.0-RC3-SNAPSHOT"
