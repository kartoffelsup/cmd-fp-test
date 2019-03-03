import io.github.kartoffelsup.Versions.arrowVersion

plugins {
  kotlin("jvm")
}

val arrowModules = setOf(
  "arrow-core-data",
  "arrow-core-extensions",
  "arrow-typeclasses",
  "arrow-extras",
  "arrow-extras-extensions",
  "arrow-effects-data",
  "arrow-effects-extensions",
  "arrow-effects-io-extensions"
)

dependencies {
  implementation(project(":modules:argparsing"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  arrowModules.forEach {
    implementation("io.arrow-kt:$it:$arrowVersion")
  }
}

tasks {
  register("bundle", Jar::class) {
    dependsOn("build")
    manifest {
      attributes["Main-Class"] = "io.github.kartoffelsup.find.FindKt"
    }
    archiveBaseName.set("find")
    from(
      configurations.runtimeClasspath.get().map {
        if (it.isDirectory)
          it
        else zipTree(it)
      }
    )
    with(get("jar") as CopySpec)
  }
}
