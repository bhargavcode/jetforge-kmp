package dev.jetforge.runtime.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ScreenDocument(
    val schemaVersion: Int = 3,
    val id: String,
    val name: String,
    val theme: ScreenTheme = ScreenTheme(),
    val dataSources: List<DataSource> = emptyList(),
    val screens: List<ScreenDef> = emptyList(),
    val startScreenId: String = "",
    val root: UiNode,
    val assets: List<AssetRef> = emptyList(),
    val publishedAt: String? = null,
)

@Serializable
data class ScreenDef(
    val id: String,
    val name: String,
    val route: String = "/",
    val root: UiNode,
    val dataSourceIds: List<String> = emptyList(),
    val emptyPath: String? = null,
    val flowX: Float? = null,
    val flowY: Float? = null,
)

@Serializable
data class ScreenTheme(
    val mode: String = "light",
    val seed: String = "purple",
)

@Serializable
data class KeyValue(
    val key: String = "",
    val value: String = "",
)

@Serializable
data class DataSource(
    val id: String,
    val name: String,
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val headerRows: List<KeyValue> = emptyList(),
    val queryRows: List<KeyValue> = emptyList(),
    val bodyMode: String = "none",
    val body: String? = null,
    val formRows: List<KeyValue> = emptyList(),
    val mock: JsonElement? = null,
    val fallbackToMock: Boolean = false,
    val simulateFailure: Boolean = false,
)

@Serializable
data class ClickAction(
    val type: String = "none",
    val screenId: String? = null,
    val params: Map<String, String> = emptyMap(),
    val url: String? = null,
    val formId: String? = null,
    val dataSourceId: String? = null,
)

@Serializable
data class Interaction(
    val event: String = "tap",
    val action: ClickAction = ClickAction(),
)

@Serializable
data class AssetRef(
    val id: String,
    val name: String,
    val kind: String = "image",
    val mime: String = "image/jpeg",
    val url: String,
)

@Serializable
data class FormFieldSpec(
    val formId: String,
    val name: String,
    val validation: ValidationRule? = null,
)

@Serializable
data class ValidationRule(
    val required: Boolean = false,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val message: String = "Invalid value",
)

@Serializable
data class UiNode(
    val id: String,
    val type: String,
    val props: JsonObject = JsonObject(emptyMap()),
    val modifiers: ModifierSpec = ModifierSpec(),
    val animation: EnterAnimation? = null,
    val bindings: Map<String, String> = emptyMap(),
    val children: List<UiNode> = emptyList(),
    val slot: String? = null,
    val itemBinding: String? = null,
    val onClick: ClickAction? = null,
    val interactions: List<Interaction> = emptyList(),
    val formField: FormFieldSpec? = null,
    val visibleWhen: String? = null,
)

@Serializable
data class ModifierSpec(
    val fillMaxWidth: Boolean = false,
    val fillMaxHeight: Boolean = false,
    val widthDp: Int? = null,
    val heightDp: Int? = null,
    val weight: Float? = null,
    val padding: PaddingSpec? = null,
    val clip: String? = null,
)

@Serializable
data class PaddingSpec(
    val all: Int? = null,
    val start: Int? = null,
    val top: Int? = null,
    val end: Int? = null,
    val bottom: Int? = null,
)

@Serializable
data class EnterAnimation(
    val type: String = "none",
    val durationMs: Int = 280,
    val delayMs: Int = 0,
    val staggerMs: Int = 0,
)

fun UiNode.resolvedInteractions(): List<Interaction> {
    if (interactions.isNotEmpty()) return interactions
    val click = onClick
    return if (click != null && click.type != "none") {
        listOf(Interaction(event = "tap", action = click))
    } else {
        emptyList()
    }
}

fun UiNode.actionFor(event: String): ClickAction? {
    resolvedInteractions().firstOrNull { it.event == event }?.action?.let { return it }
    return if (event == "tap") onClick else null
}

fun UiNode.hasGestures(): Boolean = resolvedInteractions().any { it.action.type != "none" }

fun UiNode.hasExtraGestures(): Boolean =
    resolvedInteractions().any { it.event != "tap" && it.action.type != "none" }

internal fun ScreenDocument.screenById(id: String?): ScreenDef? {
    if (screens.isEmpty()) {
        return ScreenDef(
            id = startScreenId.ifBlank { "main" },
            name = name,
            root = root,
            dataSourceIds = dataSources.map { it.id },
        )
    }
    return screens.find { it.id == id } ?: screens.find { it.id == startScreenId } ?: screens.first()
}
