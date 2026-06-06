plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "io.github.cokit"
    version = "0.1.0-SNAPSHOT"
}

tasks.register("test") {
    group = "verification"
    description = "Runs all JVM unit tests in CoKit modules."
    dependsOn(
        subprojects.map { project ->
            "${project.path}:jvmTest"
        },
    )
}
