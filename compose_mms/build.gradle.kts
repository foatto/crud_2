import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val composeVersion: String by project

val androidCompileSdk: String by project
val androidTargetSdk: String by project
val androidMinSdk: String by project

plugins {
    kotlin("multiplatform")

    id("com.android.application")

    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "compose_mms"
        browser {}
        binaries.executable()
    }

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = kotlinJvmTarget
            }
        }
//        @OptIn(ExperimentalKotlinGradlePluginApi::class)
//        compilerOptions {
//            jvmTarget.set(JvmTarget.JVM_17)
//        }
    }

    jvm {
        val main by compilations.getting {
            kotlinOptions {
                languageVersion = kotlinLanguageVersion
                apiVersion = kotlinApiVersion
                jvmTarget = kotlinJvmTarget
                freeCompilerArgs = listOf("-Xjsr305=strict")
                suppressWarnings = isBuildSupressWarning.toBoolean()
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":compose"))
            api(project(":common_mms"))
        }
    }
}

android {
    namespace = "foatto.compose_mms"

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
        applicationId = "foatto.compose_mms"
        minSdk = androidMinSdk.toInt()
        targetSdk = androidTargetSdk.toInt()
        versionCode = 1
        versionName = "1.0"
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

compose.desktop {
    application {
        mainClass = "foatto.compose_mms.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "foatto.compose_mms"
            packageVersion = "1.0.0"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec> {
    binaryenArgs = mutableListOf(
        // Required flags:
        "--enable-gc",
        "--enable-reference-types",
        "--enable-exception-handling",
        "--enable-bulk-memory",
        "--enable-nontrapping-float-to-int",

        // Optional flags (can be removed):
        "--inline-functions-with-loops",
//        "--traps-never-happen",
//        "--fast-math",
//        "-O3",
//        "-O3",
//        "--gufa",
//        "-O3",
//        "-O3",
//        "-Oz",
    )
}

tasks {
    build {
        doLast {
            copy {
                from("build/dist/wasmJs/productionExecutable")
                into("/home/foatto/MMSServerSpring/web_2")
            }

            delete("/home/foatto/MMSDesktop/lib")
            copy {
                from("build/compose/binaries/main/app/foatto.compose_mms/lib/app")
                into("/home/foatto/MMSDesktop/lib")
            }

/* native distributable
            delete("/home/foatto/MMSDesktop/bin")
            delete("/home/foatto/MMSDesktop/lib")
            copy {
                from("build/compose/binaries/main/app/foatto.compose_mms")
                into("/home/foatto/MMSDesktop")
            }
*/
        }
    }
}
