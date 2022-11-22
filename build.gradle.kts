import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21" apply false
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
                jvmTarget = "11"
                freeCompilerArgs + "-Xinline-classes"
            }
        }
    }
}
