import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val hasAndroidSdk = rootProject.extra["hasAndroidSdk"] as Boolean
if (hasAndroidSdk) {
    apply(plugin = "com.android.application")
}

kotlin {
    jvm("desktop")
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
            baseName = "Sample"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":jetforge"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        if (hasAndroidSdk) {
            androidMain.dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}

if (hasAndroidSdk) {
    extensions.configure<com.android.build.gradle.internal.dsl.BaseAppModuleExtension>("android") {
        namespace = "dev.jetforge.sample"
        compileSdk = 35
        defaultConfig {
            applicationId = "dev.jetforge.sample"
            minSdk = 26
            targetSdk = 35
            versionCode = 1
            versionName = "0.1.0"
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.jetforge.sample.MainKt"
    }
}
