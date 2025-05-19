rootProject.name = "crud"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    val kotlinVersion: String by settings
    val agpVersion: String by settings
    val composeVersion: String by settings
    val springDependencyManagementPluginVersion: String by settings
    val springBootVersion: String by settings

    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("multiplatform").version(kotlinVersion)
        kotlin("android").version(kotlinVersion)
        kotlin("jvm").version(kotlinVersion)

        kotlin("plugin.serialization").version(kotlinVersion)

        id("com.android.application").version(agpVersion)
        id("com.android.library").version(agpVersion)

        id("org.jetbrains.compose").version(composeVersion)
        id("org.jetbrains.kotlin.plugin.compose").version(kotlinVersion)

        kotlin("plugin.spring").version(kotlinVersion)
        kotlin("plugin.jpa").version(kotlinVersion)

        id("io.spring.dependency-management").version(springDependencyManagementPluginVersion)
        id("org.springframework.boot").version(springBootVersion)
    }
}

include(
    ":common",
    ":common_mms",

    ":compose",
    ":compose_mms",

    ":server",
    ":server_mms",
)



