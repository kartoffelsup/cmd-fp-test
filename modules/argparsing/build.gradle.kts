import io.github.kartoffelsup.Versions.arrowVersion
import io.github.kartoffelsup.Versions.kotestVersion

plugins {
  kotlin("jvm")
}

val arrowModules = setOf(
  "arrow-core"
)

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  arrowModules.forEach {
    implementation("io.arrow-kt:$it:$arrowVersion")
  }

  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion") {
    exclude(group = "io.arrow-kt")
    exclude(group = "org.jetbrains.kotlin")
  }

  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion") {
    exclude(group = "io.arrow-kt")
    exclude(group = "org.jetbrains.kotlin")
  }
  testImplementation("org.jetbrains.kotlin:kotlin-reflect")
}
