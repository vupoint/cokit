import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.artifacts.ProjectDependency
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.kover)
}

allprojects {
    group = providers.gradleProperty("GROUP").orElse("io.github.vupoint.cokit").get()
    version = providers.gradleProperty("VERSION_NAME")
        .orElse(providers.gradleProperty("cokitVersion"))
        .orElse("0.1.0-SNAPSHOT")
        .get()
}

val bomProjectPath = ":cokit-bom"

// Publishing is opt-out: every library subproject ships unless it is listed here.
val nonPublishedProjectPaths = setOf(
    ":cokit-sample-cli",
)

val publishedLibraryProjectPaths = provider {
    subprojects
        .map { subproject -> subproject.path }
        .filterNot { projectPath -> projectPath in nonPublishedProjectPaths || projectPath == bomProjectPath }
        .toSet()
}

val publishedArtifactProjectPaths = provider {
    publishedLibraryProjectPaths.get() + bomProjectPath
}

val publishedLibraryProjects = provider {
    publishedLibraryProjectPaths.get().map { path -> project(path) }
}

val publishedArtifactProjects = provider {
    publishedArtifactProjectPaths.get().map { path -> project(path) }
}

subprojects {
    if (path != bomProjectPath) {
        apply(plugin = "org.jetbrains.kotlinx.kover")
    }
}

configure(publishedArtifactProjects.get()) {
    apply(plugin = "com.vanniktech.maven.publish")

    // Keep common Maven Central POM developer metadata in one root-owned block.
    extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
        pom {
            developers {
                developer {
                    id.set("vupoint")
                    name.set("Taegyeong Kim")
                    email.set("vupoint@users.noreply.github.com")
                    url.set("https://github.com/vupoint")
                    organization.set("vupoint")
                    organizationUrl.set("https://github.com/vupoint")
                }
            }
        }
    }
}

configure(publishedLibraryProjects.get()) {
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension>("kotlin") {
            extensions.configure<AbiValidationMultiplatformExtension>("abiValidation") {
                @OptIn(ExperimentalAbiValidation::class)
                enabled.set(true)
                @OptIn(ExperimentalAbiValidation::class)
                legacyDump.referenceDumpDir.set(layout.projectDirectory.dir("abi"))
            }
        }
    }
}

dependencies {
    subprojects.forEach { subproject ->
        if (subproject.path != bomProjectPath) {
            add("kover", project(subproject.path))
        }
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

// CI should publish every library artifact as a set while keeping samples excluded.
tasks.register("publishAndReleaseLibrariesToMavenCentral") {
    group = "publishing"
    description = "Publishes and automatically releases all CoKit library artifacts to Maven Central without publishing the sample CLI."
    dependsOn(
        publishedArtifactProjectPaths.get().map { projectPath ->
            "$projectPath:publishAndReleaseToMavenCentral"
        },
    )
}

val cokitBomPomFile = layout.projectDirectory.file("cokit-bom/build/publications/maven/pom-default.xml")

val expectedCokitBomConstraints = publishedLibraryProjects.map { projects ->
    projects.flatMap { publishedProject ->
        listOf(
            publishedProject.name,
            "${publishedProject.name}-jvm",
        ).map { artifactId ->
            "${publishedProject.group}:$artifactId:${publishedProject.version}"
        }
    }
}

tasks.register<CokitBomConstraintsTask>("checkCokitBomConstraints") {
    group = "verification"
    description = "Checks the generated CoKit BOM constrains exactly the published library modules."
    dependsOn("$bomProjectPath:generatePomFileForMavenPublication")

    pomFile.set(cokitBomPomFile)
    expectedConstraints.set(expectedCokitBomConstraints)
}

tasks.register("checkMavenCentralPublishingConfiguration") {
    group = "verification"
    description = "Checks Maven Central publication setup and sample exclusion."
    dependsOn(
        publishedLibraryProjectPaths.get().flatMap { projectPath ->
            listOf(
                "$projectPath:checkPomFileForJvmPublication",
                "$projectPath:checkPomFileForKotlinMultiplatformPublication",
            )
        } + listOf("checkCokitBomConstraints"),
    )

    doLast {
        val expected = publishedArtifactProjectPaths.get()
        val actual = subprojects
            .filter { subproject -> subproject.plugins.hasPlugin("com.vanniktech.maven.publish") }
            .map { subproject -> subproject.path }
            .toSet()

        check(actual == expected) {
            "Unexpected Maven publication projects. expected=${expected.sorted()} actual=${actual.sorted()}"
        }
        nonPublishedProjectPaths.forEach { projectPath ->
            check(!project(projectPath).plugins.hasPlugin("com.vanniktech.maven.publish")) {
                "$projectPath must stay excluded from Maven Central publications."
            }
        }

        check(bomProjectPath !in publishedLibraryProjectPaths.get()) {
            "$bomProjectPath must be published as a BOM artifact, not treated as a Kotlin library module."
        }

        publishedArtifactProjects.get().forEach { publishedProject ->
            check(publishedProject.plugins.hasPlugin("maven-publish")) {
                "${publishedProject.path} must have maven-publish applied by the publishing plugin."
            }
            val publishing = publishedProject.extensions.getByType(PublishingExtension::class)
            check(publishing.publications.withType(MavenPublication::class.java).isNotEmpty()) {
                "${publishedProject.path} has no Maven publications."
            }
            publishing.publications.withType(MavenPublication::class.java).forEach { publication ->
                check(!publication.pom.description.orNull.isNullOrBlank()) {
                    "${publishedProject.path}:${publication.name} must set a POM description."
                }
                check(publication.pom.url.orNull == "https://github.com/vupoint/cokit") {
                    "${publishedProject.path}:${publication.name} must set the project URL."
                }
            }
        }

        check(tasks.findByName("publishAndReleaseLibrariesToMavenCentral") != null) {
            "Root publishAndReleaseLibrariesToMavenCentral task must be available for CI automation."
        }
    }
}

val allowedMainProjectDependencies = mapOf(
    ":cokit-protocol" to emptySet<String>(),
    ":cokit-rpc" to setOf(":cokit-protocol"),
    ":cokit-client-api" to setOf(":cokit-protocol", ":cokit-rpc"),
    ":cokit-client" to setOf(":cokit-client-api", ":cokit-protocol", ":cokit-rpc"),
    ":cokit-transport-stdio" to setOf(":cokit-rpc"),
    ":cokit-transport-websocket" to setOf(":cokit-rpc"),
    ":cokit-testing" to setOf(":cokit-protocol", ":cokit-rpc"),
    ":cokit-bom" to emptySet(),
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

val publicApiSourceRoots = listOf(
    "cokit-client-api/src/commonMain/kotlin",
    "cokit-client-api/src/jvmMain/kotlin",
).map { path -> layout.projectDirectory.dir(path) }

val checkPublicApiExposure = tasks.register<CokitPublicApiExposureTask>("checkPublicApiExposure") {
    group = "verification"
    description = "Checks primary client APIs do not expose raw JSON or JSON-RPC envelope types."

    sourceFiles.from(
        publicApiSourceRoots.map { sourceRoot ->
            fileTree(sourceRoot) {
                include("**/*.kt")
            }
        },
    )
    rootDirectory.set(layout.projectDirectory)
}

val primaryDocsAlignmentFiles = listOf(
    "README.md",
    "docs/getting-started.md",
    "cokit-sample-cli/src/main/kotlin/io/github/vupoint/cokit/sample/cli/Main.kt",
).map { path -> layout.projectDirectory.file(path) }

val checkPrimaryApiDocsAlignment = tasks.register("checkPrimaryApiDocsAlignment") {
    group = "verification"
    description = "Checks primary docs and samples use typed CoKit APIs instead of raw protocol examples."

    inputs.files(primaryDocsAlignmentFiles)

    doLast {
        val violations = CokitPrimaryDocsAlignment.findViolations(
            primaryDocsAlignmentFiles.map { file -> file.asFile },
            rootDir,
        )
        check(violations.isEmpty()) {
            buildString {
                appendLine("Primary docs and samples must use typed CoKit APIs.")
                appendLine("Keep raw app-server method strings in protocol compatibility docs only.")
                appendLine("Use StdioCodexTransport defaults instead of direct stdio command lists in primary examples.")
                violations.forEach { violation ->
                    appendLine(
                        "${violation.relativePath}:${violation.lineNumber}: ${violation.reason}: ${violation.match}",
                    )
                }
            }
        }
    }
}

subprojects {
    tasks.matching { task -> task.name == "check" }.configureEach {
        dependsOn(rootProject.tasks.named("validateModuleBoundaries"))
        dependsOn(checkPublicApiExposure)
        dependsOn(checkPrimaryApiDocsAlignment)
    }
}
