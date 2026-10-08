package io.github.vupoint.cokit.client.models

import io.github.vupoint.cokit.client.CodexCursor
import io.github.vupoint.cokit.client.ModelName
import io.github.vupoint.cokit.client.ReasoningEffort
import kotlinx.serialization.Serializable

/** Identifier of a model catalog entry, distinct from the [ModelName] sent when selecting a model. */
@Serializable
@JvmInline
value class ModelCatalogId(val value: String)

/** Model input capability reported by the catalog, such as text or image; unknown strings are retained. */
@Serializable
@JvmInline
value class InputModality(val value: String) {
    companion object {
        val Text = InputModality("text")
        val Image = InputModality("image")
    }
}

/** Queries the model catalog with optional hidden entries and paging; null options leave server defaults in effect. */
@Serializable
data class ModelListParams(
    val cursor: CodexCursor? = null,
    val includeHidden: Boolean? = null,
    val limit: Int? = null,
)

/** One page of model catalog entries; null [nextCursor] means no continuation was reported. */
@Serializable
data class ModelListResult(
    val data: List<ModelCatalogEntry> = emptyList(),
    val nextCursor: CodexCursor? = null,
)

/**
 * Server-reported model selection metadata and capabilities. Availability may depend on the authenticated account and server configuration.
 *
 * @property model Model selector used by thread and turn requests.
 * @property supportedReasoningEfforts Server-reported supported effort choices.
 * @property inputModalities Supported input kinds; defaults provide compatibility when the server omits this field.
 * @property serviceTiers Offered service-tier choices.
 * @property upgradeInfo Optional migration or retirement information.
 */
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

/** One supported reasoning effort with a server-provided explanation for presentation. */
@Serializable
data class ModelReasoningEffortOption(
    val reasoningEffort: ReasoningEffort,
    val description: String,
)

/** Server-provided explanation of model availability for display by an application. */
@Serializable
data class ModelAvailabilityNux(
    val message: String,
)

/** Catalog service-tier selector and its presentation metadata. */
@Serializable
data class ModelServiceTier(
    val id: String,
    val name: String,
    val description: String,
)

/** Server recommendation for migrating to another model, with optional explanatory copy and retirement timestamp. */
@Serializable
data class ModelUpgradeInfo(
    val model: ModelName,
    val modelLink: String? = null,
    val migrationMarkdown: String? = null,
    val upgradeCopy: String? = null,
    val retirementAt: io.github.vupoint.cokit.client.CodexTimestamp? = null,
)

/** Empty parameters for reading capabilities of the active model provider. */
@Serializable
data object ModelProviderCapabilitiesReadParams

/** Server-reported provider support for web search, image generation, and namespace tools. */
@Serializable
data class ModelProviderCapabilities(
    val webSearch: Boolean,
    val imageGeneration: Boolean,
    val namespaceTools: Boolean,
)

/** Cyber access programs reported for a model; this record does not enroll the account in a program. */
@Serializable
data class ModelAccessPrograms(val cyber: List<CyberAccessProgram>)

/** Server-defined cyber access program name; unknown wire strings are retained. */
@Serializable
@JvmInline
value class CyberAccessProgram(val value: String) {
    companion object {
        val Standard = CyberAccessProgram("standard")
        val DaybreakBlue = CyberAccessProgram("daybreakBlue")
        val DaybreakRed = CyberAccessProgram("daybreakRed")
    }
}

/** Model's server-reported multi-agent support version, including disabled; unknown strings are retained. */
@Serializable
@JvmInline
value class MultiAgentVersion(val value: String) {
    companion object {
        val Disabled = MultiAgentVersion("disabled")
        val V1 = MultiAgentVersion("v1")
        val V2 = MultiAgentVersion("v2")
    }
}
