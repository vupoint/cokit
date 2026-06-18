package io.github.cokit.client.models

import io.github.cokit.client.CodexCursor
import io.github.cokit.client.ModelName
import io.github.cokit.client.ReasoningEffort
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ModelCatalogId(val value: String)

@Serializable
@JvmInline
value class InputModality(val value: String) {
    companion object {
        val Text = InputModality("text")
        val Image = InputModality("image")
    }
}

@Serializable
data class ModelListParams(
    val cursor: CodexCursor? = null,
    val includeHidden: Boolean? = null,
    val limit: Int? = null,
)

@Serializable
data class ModelListResult(
    val data: List<ModelCatalogEntry> = emptyList(),
    val nextCursor: CodexCursor? = null,
)

@Serializable
data class ModelCatalogEntry(
    val id: ModelCatalogId,
    val model: ModelName,
    val displayName: String,
    val description: String,
    val hidden: Boolean,
    val isDefault: Boolean,
    val defaultReasoningEffort: ReasoningEffort,
    val supportedReasoningEfforts: List<ModelReasoningEffortOption>,
    val additionalSpeedTiers: List<String> = emptyList(),
    val availabilityNux: ModelAvailabilityNux? = null,
    val defaultServiceTier: String? = null,
    val inputModalities: List<InputModality> = listOf(InputModality.Text, InputModality.Image),
    val serviceTiers: List<ModelServiceTier> = emptyList(),
    val supportsPersonality: Boolean = false,
    val upgrade: String? = null,
    val upgradeInfo: ModelUpgradeInfo? = null,
)

@Serializable
data class ModelReasoningEffortOption(
    val reasoningEffort: ReasoningEffort,
    val description: String,
)

@Serializable
data class ModelAvailabilityNux(
    val message: String,
)

@Serializable
data class ModelServiceTier(
    val id: String,
    val name: String,
    val description: String,
)

@Serializable
data class ModelUpgradeInfo(
    val model: ModelName,
    val modelLink: String? = null,
    val migrationMarkdown: String? = null,
    val upgradeCopy: String? = null,
)

@Serializable
data object ModelProviderCapabilitiesReadParams

@Serializable
data class ModelProviderCapabilities(
    val webSearch: Boolean,
    val imageGeneration: Boolean,
    val namespaceTools: Boolean,
)
