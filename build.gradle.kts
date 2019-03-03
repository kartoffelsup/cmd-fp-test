plugins {
  kotlin("jvm") version "1.3.21" apply false
}

subprojects {
  repositories {
    mavenLocal()
    mavenCentral()
  }

  tasks {
    withType(Test::class) {
      useJUnitPlatform { }
    }
  }
}
