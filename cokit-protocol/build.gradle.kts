plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir("src/commonMain/generated")
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

val schemaProvenanceFile = layout.projectDirectory.file("src/commonMain/resources/codex-schema-provenance.properties")
val stableSchemaCommand = "codex app-server generate-json-schema --out build/generated/codex-schema/stable"
val experimentalSchemaCommand =
    "codex app-server generate-json-schema --out build/generated/codex-schema/experimental --experimental"

val generateStableCodexSchema by tasks.registering(CodexSchemaGenerateTask::class) {
    outputDirectory.set(layout.buildDirectory.dir("generated/codex-schema/stable"))
    experimental.set(false)
    provenanceFile.set(schemaProvenanceFile)
    provenanceCommandKey.set("stableCommand")
    recordedCommand.set(stableSchemaCommand)
}

val generateExperimentalCodexSchema by tasks.registering(CodexSchemaGenerateTask::class) {
    outputDirectory.set(layout.buildDirectory.dir("generated/codex-schema/experimental"))
    experimental.set(true)
    provenanceFile.set(schemaProvenanceFile)
    provenanceCommandKey.set("experimentalCommand")
    recordedCommand.set(experimentalSchemaCommand)
}

tasks.register("generateCodexSchema") {
    group = "codex"
    description = "Generates stable and experimental codex app-server JSON Schema files."
    dependsOn(generateStableCodexSchema, generateExperimentalCodexSchema)
}
