package io.github.cokit.client

import io.github.cokit.client.extensions.AppId
import io.github.cokit.client.extensions.HookEventName
import io.github.cokit.client.plugins.AppSummary
import io.github.cokit.client.plugins.MarketplaceAddParams
import io.github.cokit.client.plugins.MarketplaceRemoveParams
import io.github.cokit.client.plugins.MarketplaceUpgradeParams
import io.github.cokit.client.plugins.PluginAuthPolicy
import io.github.cokit.client.plugins.PluginAvailability
import io.github.cokit.client.plugins.PluginInstallParams
import io.github.cokit.client.plugins.PluginInstallPolicy
import io.github.cokit.client.plugins.PluginInstalledParams
import io.github.cokit.client.plugins.PluginInterface
import io.github.cokit.client.plugins.PluginListMarketplaceKind
import io.github.cokit.client.plugins.PluginListParams
import io.github.cokit.client.plugins.PluginReadParams
import io.github.cokit.client.plugins.PluginSkillReadParams
import io.github.cokit.client.plugins.PluginSource
import io.github.cokit.client.plugins.PluginUninstallParams
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PluginRpcTest {
    @Test
    fun pluginListDescriptorSendsDiscoveryFiltersAndDecodesMarketplaceSummary() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Plugin.List,
                PluginListParams(
                    cwds = listOf(CodexHostPath("/path/to/project")),
                    marketplaceKinds = listOf(
                        PluginListMarketplaceKind.Local,
                        PluginListMarketplaceKind.SharedWithMe,
                    ),
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("plugin/list", request.method)
        val params = request.params!!.jsonObject
        assertEquals("/path/to/project", params["cwds"]!!.jsonArray.single().jsonPrimitive.contentOrNull)
        assertEquals(
            listOf("local", "shared-with-me"),
            params["marketplaceKinds"]!!.jsonArray.map { it.jsonPrimitive.contentOrNull },
        )

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put(
                        "featuredPluginIds",
                        buildJsonArray {
                            add("plugin_review")
                        },
                    )
                    put(
                        "marketplaceLoadErrors",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("marketplacePath", "/path/to/project/.codex/marketplace.json")
                                    put("message", "Invalid marketplace entry")
                                },
                            )
                        },
                    )
                    put(
                        "marketplaces",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("name", "local")
                                    put("path", "/path/to/project/.codex/marketplace.json")
                                    put(
                                        "interface",
                                        buildJsonObject {
                                            put("displayName", "Local Marketplace")
                                        },
                                    )
                                    put(
                                        "plugins",
                                        buildJsonArray {
                                            add(
                                                buildPluginSummaryJson(
                                                    source = buildJsonObject {
                                                        put("type", "local")
                                                        put("path", "/path/to/project/.codex/plugins/review")
                                                    },
                                                ),
                                            )
                                        },
                                    )
                                },
                            )
                        },
                    )
                },
            ),
        )

        val decoded = result.await()
        assertEquals(listOf("plugin_review"), decoded.featuredPluginIds)
        assertEquals("Invalid marketplace entry", decoded.marketplaceLoadErrors.single().message)
        assertEquals(
            CodexHostPath("/path/to/project/.codex/marketplace.json"),
            decoded.marketplaceLoadErrors.single().marketplacePath,
        )

        val marketplace = decoded.marketplaces.single()
        assertEquals("local", marketplace.name)
        assertEquals("Local Marketplace", marketplace.interfaceMetadata?.displayName)
        assertEquals(CodexHostPath("/path/to/project/.codex/marketplace.json"), marketplace.path)

        val plugin = marketplace.plugins.single()
        assertEquals("plugin_review", plugin.id)
        assertEquals("review-helper", plugin.name)
        assertEquals(true, plugin.installed)
        assertEquals(true, plugin.enabled)
        assertEquals(PluginAuthPolicy.OnInstall, plugin.authPolicy)
        assertEquals(PluginInstallPolicy.Available, plugin.installPolicy)
        assertEquals(PluginAvailability.Available, plugin.availability)
        assertEquals(listOf("review", "code"), plugin.keywords)
        assertEquals("1.2.3", plugin.localVersion)
        assertEquals("remote_plugin_review", plugin.remotePluginId)
        assertEquals(
            PluginInterface(
                displayName = "Review Helper",
                capabilities = listOf("review"),
                screenshotUrls = listOf("https://plugins.example.invalid/review/screen.png"),
                screenshots = listOf(CodexHostPath("/path/to/project/.codex/plugins/review/screen.png")),
                composerIcon = CodexHostPath("/path/to/project/.codex/plugins/review/icon.png"),
                logoUrl = "https://plugins.example.invalid/review/logo.png",
                shortDescription = "Review code",
            ),
            plugin.interfaceMetadata,
        )
        val source = assertIs<PluginSource.Local>(plugin.source)
        assertEquals(CodexHostPath("/path/to/project/.codex/plugins/review"), source.path)

        val installedResult = async {
            fixture.client.request(
                CodexRpc.Plugin.Installed,
                PluginInstalledParams(
                    cwds = listOf(CodexHostPath("/path/to/project")),
                    installSuggestionPluginNames = listOf("review-helper"),
                ),
            )
        }
        runCurrent()

        val installed = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("plugin/installed", installed.method)
        val installedParams = installed.params!!.jsonObject
        assertEquals("/path/to/project", installedParams["cwds"]!!.jsonArray.single().jsonPrimitive.contentOrNull)
        assertEquals(
            "review-helper",
            installedParams["installSuggestionPluginNames"]!!.jsonArray.single().jsonPrimitive.contentOrNull,
        )
        fixture.transport.receive(
            JsonRpcResponse(
                installed.id,
                result = buildJsonObject {
                    put("marketplaces", buildJsonArray {})
                    put("marketplaceLoadErrors", buildJsonArray {})
                },
            ),
        )
        assertEquals(emptyList(), installedResult.await().marketplaces)
    }

    @Test
    fun pluginReadAndSkillReadDescriptorsUseCurrentSchemaMethods() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val readResult = async {
            fixture.client.request(
                CodexRpc.Plugin.Read,
                PluginReadParams(
                    pluginName = "review-helper",
                    marketplacePath = CodexHostPath("/path/to/project/.codex/marketplace.json"),
                    remoteMarketplaceName = "curated",
                ),
            )
        }
        runCurrent()

        val read = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("plugin/read", read.method)
        val readParams = read.params!!.jsonObject
        assertEquals("review-helper", readParams["pluginName"]?.jsonPrimitive?.contentOrNull)
        assertEquals("/path/to/project/.codex/marketplace.json", readParams["marketplacePath"]?.jsonPrimitive?.contentOrNull)
        assertEquals("curated", readParams["remoteMarketplaceName"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                read.id,
                result = buildJsonObject {
                    put(
                        "plugin",
                        buildJsonObject {
                            put("marketplaceName", "curated")
                            put("marketplacePath", "/path/to/project/.codex/marketplace.json")
                            put("description", "Review code changes")
                            put("shareUrl", "https://plugins.example.invalid/share/review")
                            put("summary", buildPluginSummaryJson())
                            put(
                                "apps",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("id", "app_review")
                                            put("name", "Review App")
                                            put("category", "Developer Tools")
                                            put("description", "Review pull requests")
                                            put("installUrl", "https://apps.example.invalid/review/install")
                                        },
                                    )
                                },
                            )
                            put(
                                "appTemplates",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("templateId", "template_review")
                                            put("name", "Review Template")
                                            put(
                                                "materializedAppIds",
                                                buildJsonArray {
                                                    add("app_review")
                                                },
                                            )
                                            put("canonicalConnectorId", "connector_review")
                                            put("reason", "NO_ACTIVE_WORKSPACE")
                                        },
                                    )
                                },
                            )
                            put(
                                "hooks",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("key", "review-stop")
                                            put("eventName", "stop")
                                        },
                                    )
                                },
                            )
                            put(
                                "mcpServers",
                                buildJsonArray {
                                    add("github")
                                },
                            )
                            put(
                                "skills",
                                buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("name", "review-helper")
                                            put("description", "Review code changes")
                                            put("enabled", true)
                                            put("shortDescription", "Review changes")
                                            put("path", "/path/to/project/.codex/plugins/review/skills/review-helper")
                                        },
                                    )
                                },
                            )
                        },
                    )
                },
            ),
        )

        val detail = readResult.await().plugin
        assertEquals("curated", detail.marketplaceName)
        assertEquals("Review code changes", detail.description)
        assertEquals("review-helper", detail.summary.name)
        assertEquals(AppId("app_review"), detail.apps.single().id)
        assertEquals("Review App", detail.apps.single().name)
        assertEquals("template_review", detail.appTemplates.single().templateId)
        assertEquals(HookEventName.Stop, detail.hooks.single().eventName)
        assertEquals("github", detail.mcpServers.single())
        assertEquals("review-helper", detail.skills.single().name)

        val skillResult = async {
            fixture.client.request(
                CodexRpc.Plugin.ReadSkill,
                PluginSkillReadParams(
                    remoteMarketplaceName = "curated",
                    remotePluginId = "remote_plugin_review",
                    skillName = "review-helper",
                ),
            )
        }
        runCurrent()

        val skill = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("plugin/skill/read", skill.method)
        val skillParams = skill.params!!.jsonObject
        assertEquals("curated", skillParams["remoteMarketplaceName"]?.jsonPrimitive?.contentOrNull)
        assertEquals("remote_plugin_review", skillParams["remotePluginId"]?.jsonPrimitive?.contentOrNull)
        assertEquals("review-helper", skillParams["skillName"]?.jsonPrimitive?.contentOrNull)
        fixture.transport.receive(
            JsonRpcResponse(
                skill.id,
                result = buildJsonObject {
                    put("contents", "# Review Helper")
                },
            ),
        )
        assertEquals("# Review Helper", skillResult.await().contents)
    }

    @Test
    fun pluginInstallUninstallAndMarketplaceDescriptorsUseCurrentSchemaMethods() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val addResult = async {
            fixture.client.request(
                CodexRpc.Marketplace.Add,
                MarketplaceAddParams(
                    source = "https://github.com/example/plugins.git",
                    refName = "main",
                    sparsePaths = listOf("marketplace.json"),
                ),
            )
        }
        runCurrent()

        val add = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("marketplace/add", add.method)
        val addParams = add.params!!.jsonObject
        assertEquals("https://github.com/example/plugins.git", addParams["source"]?.jsonPrimitive?.contentOrNull)
        assertEquals("main", addParams["refName"]?.jsonPrimitive?.contentOrNull)
        assertEquals("marketplace.json", addParams["sparsePaths"]!!.jsonArray.single().jsonPrimitive.contentOrNull)
        fixture.transport.receive(
            JsonRpcResponse(
                add.id,
                result = buildJsonObject {
                    put("alreadyAdded", false)
                    put("installedRoot", "/path/to/project/.codex/marketplaces/example")
                    put("marketplaceName", "example")
                },
            ),
        )
        assertEquals("example", addResult.await().marketplaceName)

        val removeResult = async {
            fixture.client.request(
                CodexRpc.Marketplace.Remove,
                MarketplaceRemoveParams(marketplaceName = "example"),
            )
        }
        runCurrent()

        val remove = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("marketplace/remove", remove.method)
        assertEquals("example", remove.params!!.jsonObject["marketplaceName"]?.jsonPrimitive?.contentOrNull)
        fixture.transport.receive(
            JsonRpcResponse(
                remove.id,
                result = buildJsonObject {
                    put("marketplaceName", "example")
                    put("installedRoot", "/path/to/project/.codex/marketplaces/example")
                },
            ),
        )
        assertEquals(CodexHostPath("/path/to/project/.codex/marketplaces/example"), removeResult.await().installedRoot)

        val upgradeResult = async {
            fixture.client.request(
                CodexRpc.Marketplace.Upgrade,
                MarketplaceUpgradeParams(marketplaceName = "example"),
            )
        }
        runCurrent()

        val upgrade = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("marketplace/upgrade", upgrade.method)
        assertEquals("example", upgrade.params!!.jsonObject["marketplaceName"]?.jsonPrimitive?.contentOrNull)
        fixture.transport.receive(
            JsonRpcResponse(
                upgrade.id,
                result = buildJsonObject {
                    put(
                        "selectedMarketplaces",
                        buildJsonArray {
                            add("example")
                        },
                    )
                    put(
                        "upgradedRoots",
                        buildJsonArray {
                            add("/path/to/project/.codex/marketplaces/example")
                        },
                    )
                    put(
                        "errors",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("marketplaceName", "broken")
                                    put("message", "Could not fetch")
                                },
                            )
                        },
                    )
                },
            ),
        )
        assertEquals("broken", upgradeResult.await().errors.single().marketplaceName)

        val installResult = async {
            fixture.client.request(
                CodexRpc.Plugin.Install,
                PluginInstallParams(
                    pluginName = "review-helper",
                    marketplacePath = CodexHostPath("/path/to/project/.codex/marketplace.json"),
                    remoteMarketplaceName = "curated",
                ),
            )
        }
        runCurrent()

        val install = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("plugin/install", install.method)
        val installParams = install.params!!.jsonObject
        assertEquals("review-helper", installParams["pluginName"]?.jsonPrimitive?.contentOrNull)
        assertEquals("/path/to/project/.codex/marketplace.json", installParams["marketplacePath"]?.jsonPrimitive?.contentOrNull)
        assertEquals("curated", installParams["remoteMarketplaceName"]?.jsonPrimitive?.contentOrNull)
        fixture.transport.receive(
            JsonRpcResponse(
                install.id,
                result = buildJsonObject {
                    put("authPolicy", "ON_USE")
                    put(
                        "appsNeedingAuth",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("id", "app_review")
                                    put("name", "Review App")
                                },
                            )
                        },
                    )
                },
            ),
        )
        assertEquals(PluginAuthPolicy.OnUse, installResult.await().authPolicy)
        assertEquals(AppSummary(id = AppId("app_review"), name = "Review App"), installResult.await().appsNeedingAuth.single())

        val uninstallResult = async {
            fixture.client.request(
                CodexRpc.Plugin.Uninstall,
                PluginUninstallParams(pluginId = "plugin_review"),
            )
        }
        runCurrent()

        val uninstall = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("plugin/uninstall", uninstall.method)
        assertEquals("plugin_review", uninstall.params!!.jsonObject["pluginId"]?.jsonPrimitive?.contentOrNull)
        fixture.transport.receive(JsonRpcResponse(uninstall.id, result = JsonObject(emptyMap())))
        assertEquals(CodexRpcUnit, uninstallResult.await())
    }

    private fun buildPluginSummaryJson(
        source: JsonObject = buildJsonObject {
            put("type", "remote")
        },
    ): JsonObject = buildJsonObject {
        put("id", "plugin_review")
        put("name", "review-helper")
        put("installed", true)
        put("enabled", true)
        put("authPolicy", "ON_INSTALL")
        put("installPolicy", "AVAILABLE")
        put("availability", "AVAILABLE")
        put("source", source)
        put(
            "keywords",
            buildJsonArray {
                add("review")
                add("code")
            },
        )
        put("localVersion", "1.2.3")
        put("remotePluginId", "remote_plugin_review")
        put(
            "interface",
            buildJsonObject {
                put("displayName", "Review Helper")
                put("shortDescription", "Review code")
                put("logoUrl", "https://plugins.example.invalid/review/logo.png")
                put("composerIcon", "/path/to/project/.codex/plugins/review/icon.png")
                put(
                    "capabilities",
                    buildJsonArray {
                        add("review")
                    },
                )
                put(
                    "screenshotUrls",
                    buildJsonArray {
                        add("https://plugins.example.invalid/review/screen.png")
                    },
                )
                put(
                    "screenshots",
                    buildJsonArray {
                        add("/path/to/project/.codex/plugins/review/screen.png")
                    },
                )
            },
        )
    }

    private suspend fun TestScope.connectedRpcClientFixture(
        scope: CoroutineScope,
    ): ConnectedRpcClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexRpcClient.connect(
                CodexRpcConnection(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                    scope = scope,
                ),
            )
        }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))
        return ConnectedRpcClientFixture(client.await(), transport)
    }

    private data class ConnectedRpcClientFixture(
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
