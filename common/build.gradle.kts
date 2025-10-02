import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val kotlinxSerializationVersion: String by project
val kotlinxDatetimeVersion: String by project

val androidCompileSdk: String by project
val androidMinSdk: String by project

plugins {
    kotlin("multiplatform")

    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(kotlinJvmTarget)
        }
    }

    jvm {
        compilerOptions {
            apiVersion = KotlinVersion.fromVersion(kotlinApiVersion)
            jvmTarget = JvmTarget.fromTarget(kotlinJvmTarget)
            languageVersion = KotlinVersion.fromVersion(kotlinLanguageVersion)
            suppressWarnings = isBuildSupressWarning.toBoolean()
        }
//                freeCompilerArgs = listOf("-Xjsr305=strict")
    }

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
            api("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")
        }
        jvmMain.dependencies {
            api(kotlin("reflect"))
        }
    }
}

android {
    namespace = "foatto.core"

    compileSdk = androidCompileSdk.toInt()
    defaultConfig {
        minSdk = androidMinSdk.toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
