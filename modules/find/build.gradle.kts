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
  "arrow-effects",
  "arrow-effects-extensions"
)

dependencies {
  implementation(project(":modules:argparsing"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  arrowModules.forEach {
    implementation("io.arrow-kt:$it:$arrowVersion")
  }
}
