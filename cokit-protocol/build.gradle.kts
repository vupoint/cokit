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

val generateStableCodexSchema by tasks.registering(CodexSchemaGenerateTask::class) {
    outputDirectory.set(layout.buildDirectory.dir("generated/codex-schema/stable"))
    experimental.set(false)
}

val generateExperimentalCodexSchema by tasks.registering(CodexSchemaGenerateTask::class) {
    outputDirectory.set(layout.buildDirectory.dir("generated/codex-schema/experimental"))
    experimental.set(true)
}

tasks.register("generateCodexSchema") {
    group = "codex"
    description = "Generates stable and experimental codex app-server JSON Schema files."
    dependsOn(generateStableCodexSchema, generateExperimentalCodexSchema)
}
