package io.github.vupoint.cokit.client.models

import io.github.vupoint.cokit.client.CodexCursor
import io.github.vupoint.cokit.client.ModelName
import io.github.vupoint.cokit.client.ReasoningEffort
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
    val availableAccessPrograms: ModelAccessPrograms? = null,
    val modelSpecialty: String? = null,
    val multiAgentVersion: MultiAgentVersion? = null,
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
    val retirementAt: io.github.vupoint.cokit.client.CodexTimestamp? = null,
)

@Serializable
data object ModelProviderCapabilitiesReadParams

@Serializable
data class ModelProviderCapabilities(
    val webSearch: Boolean,
    val imageGeneration: Boolean,
    val namespaceTools: Boolean,
)

@Serializable
data class ModelAccessPrograms(val cyber: List<CyberAccessProgram>)

@Serializable
@JvmInline
value class CyberAccessProgram(val value: String) {
    companion object {
        val Standard = CyberAccessProgram("standard")
        val DaybreakBlue = CyberAccessProgram("daybreakBlue")
        val DaybreakRed = CyberAccessProgram("daybreakRed")
    }
}

@Serializable
@JvmInline
value class MultiAgentVersion(val value: String) {
    companion object {
        val Disabled = MultiAgentVersion("disabled")
        val V1 = MultiAgentVersion("v1")
        val V2 = MultiAgentVersion("v2")
    }
}
