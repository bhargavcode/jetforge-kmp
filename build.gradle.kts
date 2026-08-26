import java.io.File

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidApplication) apply false
}

fun hasAndroidSdk(): Boolean {
    val env = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    if (env != null && File(env).resolve("platforms").exists()) return true
    val local = rootDir.resolve("local.properties")
    if (!local.exists()) return false
    val dir = local.readLines()
        .firstOrNull { it.startsWith("sdk.dir=") }
        ?.substringAfter("sdk.dir=")
        ?.replace("\\\\", "\\")
    return dir != null && File(dir).resolve("platforms").exists()
}

extra["hasAndroidSdk"] = hasAndroidSdk()
