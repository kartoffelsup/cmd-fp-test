plugins {
  kotlin("jvm") version "1.3.20" apply false
}

subprojects {
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

  tasks {
    withType(Test::class) {
      useJUnitPlatform { }
    }
  }
}
