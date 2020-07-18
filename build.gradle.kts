plugins {
    kotlin("jvm") version "1.3.72" apply false
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
