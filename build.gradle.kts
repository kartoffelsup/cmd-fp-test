import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10" apply false
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

        withType(KotlinCompile::class) {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
}
