package dev.jetforge.runtime.bind

import dev.jetforge.runtime.model.ClickAction
import dev.jetforge.runtime.model.DataSource
import dev.jetforge.runtime.model.ScreenDef
import dev.jetforge.runtime.model.UiNode
import dev.jetforge.runtime.model.ValidationRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

typealias BindingScope = MutableMap<String, JsonElement>

val JetForgeJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

fun JsonElement.getByPath(path: String): JsonElement? {
    val clean = path.replace("{{", "").replace("}}", "").trim()
    if (clean.isEmpty()) return null
    var current: JsonElement? = this
    for (part in clean.split(".").filter { it.isNotEmpty() }) {
        current = when (val node = current) {
            is JsonObject -> node[part]
            is JsonArray -> part.toIntOrNull()?.let { node.getOrNull(it) }
            else -> null
        }
    }
    return current
}

fun BindingScope.resolvePath(path: String): JsonElement? {
    val clean = path.replace("{{", "").replace("}}", "").trim()
    if (clean.isEmpty()) return null
    JsonObject(this).getByPath(clean)?.let { return it }
    val first = clean.substringBefore(".")
    val rest = clean.substringAfter(".", missingDelimiterValue = "")
    val root = this[first] ?: return null
    return if (rest.isEmpty()) root else root.getByPath(rest)
}

fun JsonElement.asDisplayString(): String = when (this) {
    is JsonNull -> ""
    is JsonPrimitive -> contentOrNull ?: booleanOrNull?.toString() ?: doubleOrNull?.toString() ?: ""
    else -> JetForgeJson.encodeToString(JsonElement.serializer(), this)
}

fun JsonObject.prop(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

fun JsonObject.bool(key: String, default: Boolean = false): Boolean =
    this[key]?.jsonPrimitive?.booleanOrNull ?: default

fun JsonObject.int(key: String, default: Int): Int =
    this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: default

fun interpolate(template: String, scope: BindingScope): String {
    val regex = Regex("""\{\{\s*([^}]+)\s*\}\}""")
    return regex.replace(template) { match ->
        scope.resolvePath(match.groupValues[1])?.asDisplayString().orEmpty()
    }
}

fun validateValue(value: String, rule: ValidationRule?): String? {
    if (rule == null) return null
    val trimmed = value.trim()
    if (rule.required && trimmed.isEmpty()) return rule.message.ifBlank { "This field is required." }
    rule.minLength?.let {
        if (trimmed.length < it) return rule.message.ifBlank { "Enter at least $it characters." }
    }
    rule.maxLength?.let {
        if (trimmed.length > it) return rule.message.ifBlank { "Use at most $it characters." }
    }
    rule.pattern?.let { pattern ->
        return try {
            if (!Regex(pattern).containsMatchIn(trimmed)) rule.message.ifBlank { "That value is not valid." } else null
        } catch (_: Exception) {
            rule.message.ifBlank { "That value is not valid." }
        }
    }
    return null
}

fun collectFormFields(root: UiNode): List<UiNode> {
    val out = mutableListOf<UiNode>()
    fun walk(node: UiNode) {
        if (node.formField != null) out += node
        node.children.forEach(::walk)
    }
    walk(root)
    return out
}

fun validateForm(root: UiNode, formId: String, values: Map<String, String>): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    for (node in collectFormFields(root)) {
        val field = node.formField ?: continue
        if (field.formId != formId) continue
        validateValue(values[field.name].orEmpty(), field.validation)?.let { errors[field.name] = it }
    }
    return errors
}

fun resolveActionParams(action: ClickAction, scope: BindingScope): Map<String, JsonElement> {
    return action.params.mapValues { (_, path) ->
        scope.resolvePath(path) ?: JsonPrimitive(interpolate(path, scope))
    }
}

fun sourcesForScreen(dataSources: List<DataSource>, screen: ScreenDef): List<String> {
    if (screen.dataSourceIds.isNotEmpty()) return screen.dataSourceIds
    val id = screen.emptyPath?.substringBefore(".")
    if (id != null && dataSources.any { it.id == id }) return listOf(id)
    return emptyList()
}

fun screenErrors(errors: Map<String, String>, sourceIds: List<String>): Map<String, String> {
    if (sourceIds.isEmpty()) return emptyMap()
    return sourceIds.mapNotNull { id -> errors[id]?.let { id to it } }.toMap()
}

fun interpolateSource(source: DataSource, scope: BindingScope): DataSource {
    return source.copy(
        url = interpolate(source.url, scope),
        body = source.body?.let { interpolate(it, scope) },
        headers = source.headers.mapValues { interpolate(it.value, scope) },
        headerRows = source.headerRows.map { it.copy(value = interpolate(it.value, scope)) },
        queryRows = source.queryRows.map { it.copy(value = interpolate(it.value, scope)) },
        formRows = source.formRows.map { it.copy(value = interpolate(it.value, scope)) },
    )
}

fun mergedHeaders(source: DataSource): Map<String, String> {
    val out = source.headers.toMutableMap()
    for (row in source.headerRows) {
        if (row.key.isNotBlank()) out[row.key] = row.value
    }
    return out
}

fun resolveRequestUrl(source: DataSource, baseUrl: String): String {
    val raw = source.url
    val absolute = when {
        raw.startsWith("http://") || raw.startsWith("https://") -> raw
        raw.startsWith("/") -> baseUrl.trimEnd('/') + raw
        else -> baseUrl.trimEnd('/') + "/" + raw
    }
    if (source.queryRows.none { it.key.isNotBlank() }) return absolute
    val joined = source.queryRows
        .filter { it.key.isNotBlank() }
        .joinToString("&") { row ->
            val key = encodeQuery(row.key)
            val value = encodeQuery(row.value)
            "$key=$value"
        }
    return if (absolute.contains("?")) "$absolute&$joined" else "$absolute?$joined"
}

private fun encodeQuery(value: String): String = buildString {
    for (ch in value) {
        when {
            ch.isLetterOrDigit() || ch in "-._~" -> append(ch)
            ch == ' ' -> append("%20")
            else -> append("%").append(ch.code.toString(16).uppercase().padStart(2, '0'))
        }
    }
}

fun resolveMediaUrl(url: String, baseUrl: String): String {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    val base = baseUrl.trimEnd('/')
    return if (trimmed.startsWith("/")) "$base$trimmed" else "$base/$trimmed"
}

fun mergeBindingData(base: BindingScope, overlay: BindingScope): BindingScope {
    val next: BindingScope = (base + overlay).toMutableMap()
    for ((key, mock) in base) {
        val live = overlay[key]
        if (live == null || live is JsonNull) {
            next[key] = mock
            continue
        }
        if (mock is JsonObject && live is JsonObject) {
            val merged = (mock + live).toMutableMap()
            val mockArticles = mock["articles"]
            val liveArticles = live["articles"]
            if (mockArticles is JsonArray && liveArticles !is JsonArray) {
                merged["articles"] = mockArticles
            }
            next[key] = JsonObject(merged)
        }
    }
    return next
}

fun resolveList(scope: BindingScope, path: String): JsonArray {
    val direct = scope.resolvePath(path)
    if (direct is JsonArray) return direct
    val sourceId = path.substringBefore(".")
    val source = scope[sourceId]
    if (source is JsonObject) {
        for (key in listOf("articles", "items", "results")) {
            val value = source[key]
            if (value is JsonArray) return value
        }
    }
    return JsonArray(emptyList())
}

fun computeUiState(
    loading: Boolean,
    errors: Map<String, String>,
    data: BindingScope,
    emptyPath: String?,
): String {
    if (loading) return "loading"
    if (errors.isNotEmpty()) return "error"
    if (!emptyPath.isNullOrBlank() && resolveList(data, emptyPath).isEmpty()) return "empty"
    return "ready"
}

fun isNodeVisible(visibleWhen: String?, state: String, hasFormError: Boolean): Boolean {
    val whenState = visibleWhen ?: "always"
    if (whenState == "always") return true
    if (whenState == "invalid") return hasFormError
    return whenState == state
}

fun buildFormScope(
    values: Map<String, Map<String, String>>,
    errors: Map<String, Map<String, String>>,
): JsonObject {
    val ids = values.keys + errors.keys
    return buildJsonObject {
        for (id in ids) {
            put(id, buildJsonObject {
                (values[id] ?: emptyMap()).forEach { (k, v) -> put(k, v) }
                put("errors", buildJsonObject {
                    (errors[id] ?: emptyMap()).forEach { (k, v) -> put(k, v) }
                })
            })
        }
    }
}

fun BindingScope.toJsonObject(): JsonObject = JsonObject(this)

fun mocksFrom(documentDataSources: List<DataSource>): BindingScope {
    val scope: BindingScope = mutableMapOf()
    for (source in documentDataSources) {
        scope[source.id] = source.mock ?: JsonObject(emptyMap())
    }
    return scope
}
