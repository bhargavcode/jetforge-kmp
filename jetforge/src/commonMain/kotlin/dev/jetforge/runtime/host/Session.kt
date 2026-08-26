package dev.jetforge.runtime.host

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.jetforge.runtime.bind.BindingScope
import dev.jetforge.runtime.bind.buildFormScope
import dev.jetforge.runtime.bind.computeUiState
import dev.jetforge.runtime.bind.interpolate
import dev.jetforge.runtime.bind.mergeBindingData
import dev.jetforge.runtime.bind.mocksFrom
import dev.jetforge.runtime.bind.resolveActionParams
import dev.jetforge.runtime.bind.screenErrors
import dev.jetforge.runtime.bind.sourcesForScreen
import dev.jetforge.runtime.bind.validateForm
import dev.jetforge.runtime.client.JetForgeClient
import dev.jetforge.runtime.model.ClickAction
import dev.jetforge.runtime.model.ScreenDocument
import dev.jetforge.runtime.model.UiNode
import dev.jetforge.runtime.model.screenById
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

sealed interface JetForgeLoadState {
    data object Loading : JetForgeLoadState
    data class Ready(val session: JetForgeSession) : JetForgeLoadState
    data class Failed(val message: String) : JetForgeLoadState
}

class JetForgeSession internal constructor(
    val document: ScreenDocument,
    private val client: JetForgeClient,
    private val onOpenUrl: (String) -> Unit,
) {
    var screenId by mutableStateOf(document.startScreenId.ifBlank { document.screens.firstOrNull()?.id.orEmpty() })
        private set
    var route by mutableStateOf<BindingScope>(mutableMapOf())
        private set
    var data by mutableStateOf<BindingScope>(mocksFrom(document.dataSources))
        private set
    var errors by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var loading by mutableStateOf(true)
        private set
    var formValues by mutableStateOf<Map<String, Map<String, String>>>(emptyMap())
        private set
    var formErrors by mutableStateOf<Map<String, Map<String, String>>>(emptyMap())
        private set

    private val history = ArrayDeque<String>()

    val currentScreen get() = document.screenById(screenId)!!
    val root: UiNode get() = currentScreen.root

    val runtimeData: BindingScope
        get() = mergeBindingData(mocksFrom(document.dataSources), data)

    val scopedErrors: Map<String, String>
        get() = screenErrors(errors, sourcesForScreen(document.dataSources, currentScreen))

    val uiState: String
        get() = computeUiState(loading, scopedErrors, runtimeData, currentScreen.emptyPath)

    val hasFormError: Boolean
        get() = formErrors.values.any { it.isNotEmpty() }

    val scope: BindingScope
        get() {
            val next = runtimeData.toMutableMap()
            next["route"] = JsonObject(route)
            next["forms"] = buildFormScope(formValues, formErrors)
            next["errors"] = JsonObject(scopedErrors.mapValues { (_, v) -> kotlinx.serialization.json.JsonPrimitive(v) })
            return next
        }

    suspend fun loadCurrent() {
        loading = true
        try {
            val bindScope: BindingScope = runtimeData.toMutableMap().apply {
                put("route", JsonObject(route))
                put("forms", buildFormScope(formValues, emptyMap()))
            }
            val result = client.bind(document, currentScreen, bindScope)
            data = result.data
            errors = result.errors
        } catch (error: Exception) {
            errors = mapOf((currentScreen.dataSourceIds.firstOrNull() ?: "app") to (error.message ?: "Request failed"))
        } finally {
            loading = false
        }
    }

    fun setFormValue(formId: String, name: String, value: String) {
        val group = (formValues[formId] ?: emptyMap()) + (name to value)
        formValues = formValues + (formId to group)
        val errGroup = (formErrors[formId] ?: emptyMap()) - name
        formErrors = formErrors + (formId to errGroup)
    }

    suspend fun dispatch(action: ClickAction, actionScope: BindingScope) {
        when (action.type) {
            "none", "" -> Unit
            "back" -> {
                val previous = history.removeLastOrNull() ?: return
                screenId = previous
                loadCurrent()
            }
            "retry", "callApi" -> loadCurrent()
            "openUrl" -> {
                val url = interpolate(action.url.orEmpty(), actionScope)
                if (url.isNotBlank()) onOpenUrl(url)
            }
            "submitForm" -> {
                val formId = action.formId ?: return
                val found = validateForm(root, formId, formValues[formId] ?: emptyMap())
                formErrors = formErrors + (formId to found)
                if (found.isNotEmpty()) return
                if (!action.screenId.isNullOrBlank()) {
                    history.addLast(screenId)
                    screenId = action.screenId
                }
                loadCurrent()
            }
            "navigate" -> {
                val target = action.screenId ?: return
                val params = resolveActionParams(action, actionScope)
                route = (route + params).toMutableMap()
                history.addLast(screenId)
                screenId = target
                formErrors = emptyMap()
                loadCurrent()
            }
        }
    }
}
