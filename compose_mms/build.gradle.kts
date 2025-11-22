import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExec

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
        outputModuleName = "compose_mms"
        browser {}
        binaries.executable()
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

tasks.withType<BinaryenExec> {
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
        }
    }
    // fatjar
//    val fatJar = register("fatjar") {
//        dependsOn("build", "packageUberJarForCurrentOS")
//        doLast {
//            copy {
//                from("build/compose/jars")
//                into("/home/foatto/MMSDesktop/lib")
//            }
//            delete("build/compose/jars/XXX.jar")
//        }
//    }
}
