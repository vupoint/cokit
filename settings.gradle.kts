pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "cokit"

include(
    ":cokit-protocol",
    ":cokit-rpc",
    ":cokit-client",
    ":cokit-transport-stdio",
    ":cokit-transport-websocket",
    ":cokit-testing",
    ":cokit-bom",
    ":cokit-sample-cli",
)
