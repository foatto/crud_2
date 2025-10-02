import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

plugins {
    kotlin("jvm")

    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("plugin.serialization")

    id("io.spring.dependency-management")
    id("org.springframework.boot")
}

dependencies {
    api(project(":server"))
    api(project(":common_mms"))
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
}

tasks {
    compileKotlin {
        compilerOptions {
            apiVersion = KotlinVersion.fromVersion(kotlinApiVersion)
            jvmTarget = JvmTarget.fromTarget(kotlinJvmTarget)
            languageVersion = KotlinVersion.fromVersion(kotlinLanguageVersion)
            suppressWarnings = isBuildSupressWarning.toBoolean()
        }
//                freeCompilerArgs = listOf("-Xjsr305=strict")
    }
    jar {
        enabled = true
    }
    build {
        doLast {
            delete("/home/foatto/MMSServerSpring/lib_2")
            copy {
                from("build/libs/server_mms-plain.jar", configurations["runtimeClasspath"])
                into("/home/foatto/MMSServerSpring/lib_2")
            }
        }
    }
}
