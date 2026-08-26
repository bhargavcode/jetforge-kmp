package dev.jetforge.runtime.client

import dev.jetforge.runtime.bind.JetForgeJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

internal actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    install(ContentNegotiation) { json(JetForgeJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 10_000
    }
}
