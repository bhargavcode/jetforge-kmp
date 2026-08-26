package dev.jetforge.runtime.bind

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class BindingTest {
    @Test
    fun resolvePathReadsNestedArticles() {
        val scope: BindingScope = mutableMapOf(
            "news" to buildJsonObject {
                put("articles", JsonArray(listOf(buildJsonObject { put("title", "Hello") })))
            },
        )
        val list = resolveList(scope, "news.articles")
        assertEquals(1, list.size)
        assertEquals("Hello", scope.resolvePath("news.articles.0.title")?.asDisplayString())
    }

    @Test
    fun interpolateFillsFormQuery() {
        val scope: BindingScope = mutableMapOf(
            "forms" to buildJsonObject {
                put("search", buildJsonObject { put("query", "senate") })
            },
        )
        assertEquals(
            "/api/news/us?q=senate",
            interpolate("/api/news/us?q={{forms.search.query}}", scope),
        )
    }

    @Test
    fun validationRejectsShortQuery() {
        val rule = dev.jetforge.runtime.model.ValidationRule(required = true, minLength = 3, message = "Enter at least 3 characters to search.")
        assertEquals("Enter at least 3 characters to search.", validateValue("ab", rule))
        assertEquals(null, validateValue("senate", rule))
    }

    @Test
    fun computeUiStateEmptyWhenNoArticles() {
        val data: BindingScope = mutableMapOf("news" to JsonObject(emptyMap()))
        assertEquals("empty", computeUiState(false, emptyMap(), data, "news.articles"))
        assertEquals("error", computeUiState(false, mapOf("news" to "fail"), data, "news.articles"))
        assertEquals("loading", computeUiState(true, emptyMap(), data, "news.articles"))
    }

    @Test
    fun endpointIdFromRelativeAndAbsolute() {
        val client = dev.jetforge.runtime.client.JetForgeClient(
            dev.jetforge.runtime.JetForgeConfig(baseUrl = "http://127.0.0.1:43145"),
        )
        assertEquals("us-briefing", client.resolve("us-briefing").id)
        assertTrue(client.resolve("us-briefing").screenUrl.endsWith("/api/screens/us-briefing"))
        assertEquals("us-briefing", client.resolve("http://host/api/screens/us-briefing").id)
    }

    @Test
    fun interpolateSourceFillsHeadersQueryAndForm() {
        val scope: BindingScope = mutableMapOf(
            "forms" to buildJsonObject {
                put("search", buildJsonObject { put("query", "senate") })
            },
        )
        val source = dev.jetforge.runtime.model.DataSource(
            id = "search",
            name = "Search",
            url = "/api/news/us",
            queryRows = listOf(dev.jetforge.runtime.model.KeyValue("q", "{{forms.search.query}}")),
            headerRows = listOf(dev.jetforge.runtime.model.KeyValue("X-App", "jetforge")),
            formRows = listOf(dev.jetforge.runtime.model.KeyValue("q", "{{forms.search.query}}")),
        )
        val next = interpolateSource(source, scope)
        assertEquals("senate", next.queryRows.first().value)
        assertEquals("jetforge", next.headerRows.first().value)
        assertEquals("senate", next.formRows.first().value)
        assertTrue(resolveRequestUrl(next, "http://127.0.0.1:43145").contains("q=senate"))
    }

    @Test
    fun resolveMediaUrlPrefixesStudioHost() {
        assertEquals(
            "http://127.0.0.1:43145/api/assets/1.png",
            resolveMediaUrl("/api/assets/1.png", "http://127.0.0.1:43145"),
        )
    }
}
