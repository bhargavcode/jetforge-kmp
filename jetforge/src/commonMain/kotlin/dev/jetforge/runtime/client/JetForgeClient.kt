package dev.jetforge.runtime.client

import dev.jetforge.runtime.JetForgeConfig
import dev.jetforge.runtime.bind.BindingScope
import dev.jetforge.runtime.bind.JetForgeJson
import dev.jetforge.runtime.bind.interpolateSource
import dev.jetforge.runtime.bind.mocksFrom
import dev.jetforge.runtime.bind.mergeBindingData
import dev.jetforge.runtime.bind.sourcesForScreen
import dev.jetforge.runtime.bind.toJsonObject
import dev.jetforge.runtime.model.DataSource
import dev.jetforge.runtime.model.ScreenDef
import dev.jetforge.runtime.model.ScreenDocument
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import dev.jetforge.runtime.bind.mergedHeaders
import dev.jetforge.runtime.bind.resolveRequestUrl

data class ResolvedEndpoint(
    val screenUrl: String,
    val baseUrl: String,
    val id: String,
)

data class BindResult(
    val data: BindingScope,
    val errors: Map<String, String>,
)

@Serializable
private data class BindRequest(
    val dataSources: List<DataSource>,
    val scope: JsonObject,
)

@Serializable
private data class BindResponse(
    val data: Map<String, JsonElement> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
)

class JetForgeClient(
    private val config: JetForgeConfig,
) {
    private val http = createHttpClient()

    fun resolve(endpoint: String): ResolvedEndpoint {
        val trimmed = endpoint.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
                val base = trimmed.substringBefore("/api/screens").trimEnd('/')
                    .ifBlank { config.baseUrl.trimEnd('/') }
                val id = trimmed.substringAfterLast('/').substringBefore('?')
                ResolvedEndpoint(screenUrl = trimmed, baseUrl = base, id = id)
            }
            trimmed.startsWith("/") -> ResolvedEndpoint(
                screenUrl = config.baseUrl.trimEnd('/') + trimmed,
                baseUrl = config.baseUrl.trimEnd('/'),
                id = trimmed.substringAfterLast('/'),
            )
            else -> ResolvedEndpoint(
                screenUrl = "${config.baseUrl.trimEnd('/')}/api/screens/$trimmed",
                baseUrl = config.baseUrl.trimEnd('/'),
                id = trimmed,
            )
        }
    }

    suspend fun fetchDocument(endpoint: String): ScreenDocument {
        val resolved = resolve(endpoint)
        val text = http.get(resolved.screenUrl) {
            config.headers.forEach { (k, v) -> header(k, v) }
        }.bodyAsText()
        return JetForgeJson.decodeFromString(ScreenDocument.serializer(), text)
    }

    suspend fun bind(
        document: ScreenDocument,
        screen: ScreenDef,
        scope: BindingScope,
    ): BindResult {
        val resolvedBase = config.baseUrl.trimEnd('/')
        val ids = sourcesForScreen(document.dataSources, screen)
        val sources = document.dataSources.filter { it.id in ids }.map { interpolateSource(it, scope) }
        val mocks = mocksFrom(document.dataSources)
        if (sources.isEmpty()) return BindResult(mocks, emptyMap())

        return try {
            val response = http.post("$resolvedBase/api/bind") {
                contentType(ContentType.Application.Json)
                config.headers.forEach { (k, v) -> header(k, v) }
                setBody(BindRequest(dataSources = sources, scope = scope.toJsonObject()))
            }
            if (response.status.isSuccess()) {
                val payload = JetForgeJson.decodeFromString(BindResponse.serializer(), response.bodyAsText())
                BindResult(
                    data = mergeBindingData(mocks, payload.data.toMutableMap()),
                    errors = payload.errors,
                )
            } else {
                fetchSourcesDirect(sources, mocks, resolvedBase)
            }
        } catch (_: Exception) {
            fetchSourcesDirect(sources, mocks, resolvedBase)
        }
    }

    private suspend fun fetchSourcesDirect(
        sources: List<DataSource>,
        mocks: BindingScope,
        baseUrl: String,
    ): BindResult {
        val data: BindingScope = mutableMapOf()
        val errors = mutableMapOf<String, String>()
        for (source in sources) {
            if (source.simulateFailure) {
                if (source.fallbackToMock && source.mock != null) {
                    data[source.id] = source.mock
                } else {
                    errors[source.id] = "${source.name} failed"
                }
                continue
            }
            val url = resolveRequestUrl(source, baseUrl)
            try {
                val response = http.request(url) {
                    method = HttpMethod.parse(source.method.ifBlank { "GET" }.uppercase())
                    mergedHeaders(source).forEach { (k, v) -> header(k, v) }
                    config.headers.forEach { (k, v) -> header(k, v) }
                    val mode = source.bodyMode.ifBlank {
                        if (!source.body.isNullOrBlank()) "json" else "none"
                    }
                    val skipBody = method == HttpMethod.Get || method == HttpMethod.Head || method == HttpMethod.Delete
                    if (!skipBody) {
                        when (mode) {
                            "json" -> {
                                contentType(ContentType.Application.Json)
                                setBody(source.body ?: "{}")
                            }
                            "form" -> {
                                setBody(
                                    FormDataContent(
                                        Parameters.build {
                                            source.formRows.filter { it.key.isNotBlank() }.forEach {
                                                append(it.key, it.value)
                                            }
                                        },
                                    ),
                                )
                            }
                            "multipart" -> {
                                setBody(
                                    MultiPartFormDataContent(
                                        formData {
                                            source.formRows.filter { it.key.isNotBlank() }.forEach {
                                                append(it.key, it.value)
                                            }
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
                if (!response.status.isSuccess()) error("${response.status}")
                data[source.id] = JetForgeJson.parseToJsonElement(response.bodyAsText())
            } catch (error: Exception) {
                if (source.fallbackToMock && source.mock != null) {
                    data[source.id] = source.mock
                } else {
                    errors[source.id] = error.message ?: "Request failed"
                }
            }
        }
        return BindResult(mergeBindingData(mocks, data), errors)
    }
}
