import io.github.kartoffelsup.Versions.arrowVersion
import io.github.kartoffelsup.Versions.kotlinTestVersion

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

  testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlinTestVersion") {
    exclude(group = "io.arrow-kt")
    exclude(group = "org.jetbrains.kotlin")
  }
  testImplementation("org.jetbrains.kotlin:kotlin-reflect")
}
