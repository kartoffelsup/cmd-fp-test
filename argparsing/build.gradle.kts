import io.github.kartoffelsup.Versions.arrowVersion
import io.github.kartoffelsup.Versions.kotlinTestVersion

plugins {
  kotlin("jvm")
}

val arrowModules = setOf(
  "arrow-core-data",
  "arrow-extras",
  "arrow-typeclasses"
)

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  arrowModules.forEach {
    implementation("io.arrow-kt:$it:$arrowVersion")
  }

  testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlinTestVersion")
  testImplementation("io.arrow-kt:arrow-core-extensions:$arrowVersion")
}
