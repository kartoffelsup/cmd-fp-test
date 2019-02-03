object Versions {
  const val arrowVersion = "0.9.0-SNAPSHOT"
  const val kotlinTestVersion = "3.2.1"
}

plugins {
  id("org.jetbrains.kotlin.jvm").version("1.3.20")
}

repositories {
  mavenLocal()
  mavenCentral()
  maven {
    url = uri("http://oss.jfrog.org/artifactory/oss-snapshot-local/")
    content {
      includeGroup("io.arrow-kt")
    }
  }
}

val arrowModules = setOf(
  "arrow-core-data",
  "arrow-syntax",
  "arrow-typeclasses",
  "arrow-extras",
  "arrow-extras-extensions",
  "arrow-effects",
  "arrow-effects-extensions"
)


dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

  arrowModules.forEach {
    implementation("io.arrow-kt:$it:${Versions.arrowVersion}")
  }

  testImplementation("io.kotlintest:kotlintest-runner-junit5:${Versions.kotlinTestVersion}")
}

tasks {
  withType(Test::class) {
    useJUnitPlatform { }
  }
}
