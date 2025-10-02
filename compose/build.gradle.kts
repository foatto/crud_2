import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val composeVersion: String by project
val materialIconsCoreVersion: String by project

val androidCompileSdk: String by project
val androidMinSdk: String by project

val kotlinxCoroutinesVersion: String by project
val ktorVersion: String by project

val multiplatformSettingsVersion: String by project
val kamelImageVersion: String by project
val fileKitVersion: String by project

val androidxActivityComposeVersion: String by project

plugins {
    kotlin("multiplatform")

    id("com.android.library")

    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
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
            api(compose.runtime)
            api(compose.foundation)
            api(compose.ui)
            api(compose.material3)
            api(compose.components.resources)
            api(compose.components.uiToolingPreview)
            api("org.jetbrains.compose.material:material-icons-core:${materialIconsCoreVersion}")

            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

            api("io.ktor:ktor-client-cio:${ktorVersion}")
            api("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
            api("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")

            api("com.russhwolf:multiplatform-settings:$multiplatformSettingsVersion")
            api("com.russhwolf:multiplatform-settings-no-arg:$multiplatformSettingsVersion")
            api("media.kamel:kamel-image-default:$kamelImageVersion")

            api("io.github.vinceglb:filekit-core:${fileKitVersion}")
            api("io.github.vinceglb:filekit-dialogs-compose:${fileKitVersion}")
            //api("io.github.vinceglb:filekit-coil:$fileKitVersion") - пока не нужно

            api(project(":common"))
        }
        androidMain.dependencies {
            api("androidx.activity:activity-compose:${androidxActivityComposeVersion}")
        }
        jvmMain.dependencies {
            api(compose.desktop.currentOs)  //!!! linux, windows

            api("org.jetbrains.kotlinx:kotlinx-coroutines-swing:${kotlinxCoroutinesVersion}")
        }
    }
}

android {
    namespace = "foatto.compose"

    dependencies {
        debugApi("androidx.compose.ui:ui-tooling:$composeVersion")
    }

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
        resources.srcDirs("src/commonMain/resources")
    }

    compileSdk = androidCompileSdk.toInt()
    defaultConfig {
        minSdk = androidMinSdk.toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
    }

    buildFeatures {
        compose = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

//composeCompiler {
//    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
//    reportsDestination = layout.buildDirectory.dir("compose_compiler")
//    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
//}
