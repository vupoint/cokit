package io.github.vupoint.cokit.client.auth

import io.github.vupoint.cokit.client.ExperimentalCodexApi
import kotlinx.serialization.Serializable

/** Advisory GovCloud policy result; this does not certify network configuration or authorize login. */
@ExperimentalCodexApi
@Serializable
data class BedrockGovCloudRequirementsResult(val isGovCloud: Boolean, val shouldWarn: Boolean)
