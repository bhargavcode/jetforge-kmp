package dev.jetforge.runtime.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import dev.jetforge.runtime.bind.JetForgeJson

internal actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) { json(JetForgeJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 10_000
    }
}
