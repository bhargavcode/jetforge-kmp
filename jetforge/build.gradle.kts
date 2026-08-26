import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val hasAndroidSdk = rootProject.extra["hasAndroidSdk"] as Boolean
if (hasAndroidSdk) {
    apply(plugin = "com.android.library")
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    if (hasAndroidSdk) {
        androidTarget {
            compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        }
    }

    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
        target.binaries.framework {
            baseName = "JetForge"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        if (hasAndroidSdk) {
            androidMain.dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.android)
            }
        }
    }
}

if (hasAndroidSdk) {
    extensions.configure<com.android.build.gradle.LibraryExtension>("android") {
        namespace = "dev.jetforge.runtime"
        compileSdk = 35
        defaultConfig {
            minSdk = 26
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}
