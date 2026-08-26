package dev.jetforge.runtime.client

import io.ktor.client.HttpClient

internal expect fun createHttpClient(): HttpClient
