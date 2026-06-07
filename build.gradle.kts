import org.gradle.api.artifacts.ProjectDependency

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover)
}

allprojects {
    group = "io.github.cokit"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

dependencies {
    subprojects.forEach { subproject ->
        add("kover", project(subproject.path))
    }
}

tasks.register("test") {
    group = "verification"
    description = "Runs all JVM unit tests in CoKit modules."
    subprojects.forEach { project ->
        dependsOn(project.tasks.matching { task -> task.name == "jvmTest" || task.name == "test" })
    }
}

tasks.register("coverage") {
    group = "verification"
    description = "Runs tests and generates aggregate Kover coverage reports."
    dependsOn("test", "koverHtmlReport", "koverXmlReport", "koverLog")
}

val allowedMainProjectDependencies = mapOf(
    ":cokit-protocol" to emptySet<String>(),
    ":cokit-rpc" to setOf(":cokit-protocol"),
    ":cokit-client" to setOf(":cokit-protocol", ":cokit-rpc"),
    ":cokit-transport-stdio" to setOf(":cokit-rpc"),
    ":cokit-transport-websocket" to setOf(":cokit-rpc"),
    ":cokit-testing" to setOf(":cokit-protocol", ":cokit-rpc"),
    ":cokit-sample-cli" to setOf(":cokit-client", ":cokit-transport-stdio"),
)

val productionDependencyConfigurations = setOf(
    "api",
    "implementation",
    "compileOnly",
    "runtimeOnly",
    "commonMainApi",
    "commonMainImplementation",
    "jvmMainApi",
    "jvmMainImplementation",
)

tasks.register("validateModuleBoundaries") {
    group = "verification"
    description = "Validates CoKit production module dependency boundaries."

    doLast {
        subprojects.forEach { module ->
            val allowed = allowedMainProjectDependencies[module.path]
                ?: error("No module boundary rule registered for ${module.path}")
            val actual = module.configurations
                .filter { configuration ->
                    configuration.name in productionDependencyConfigurations
                }
                .flatMap { configuration ->
                    configuration.dependencies
                        .filterIsInstance<ProjectDependency>()
                        .map { dependency -> dependency.path }
                }
                .toSet()
            val forbidden = actual - allowed
            check(forbidden.isEmpty()) {
                "${module.path} has forbidden main project dependencies: ${forbidden.sorted()}"
            }
        }
    }
}

subprojects {
    tasks.matching { task -> task.name == "check" }.configureEach {
        dependsOn(rootProject.tasks.named("validateModuleBoundaries"))
    }
}
