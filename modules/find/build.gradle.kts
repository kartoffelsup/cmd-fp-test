import io.github.kartoffelsup.Versions.arrowVersion
import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
  kotlin("jvm")
  kotlin("kapt")
}

val arrowModules = setOf(
  "arrow-core",
  "arrow-syntax",
  "arrow-fx"
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
