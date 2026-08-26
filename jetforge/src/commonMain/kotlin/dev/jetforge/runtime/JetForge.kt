package dev.jetforge.runtime

data class JetForgeConfig(
    val baseUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val defaultEndpoint: String = "us-briefing",
)

object JetForge {
    @Volatile
    var config: JetForgeConfig = JetForgeConfig(baseUrl = "http://127.0.0.1:43145")
        private set

    fun configure(config: JetForgeConfig) {
        this.config = config
    }
}
