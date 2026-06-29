plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":cokit-client-api"))
            implementation(project(":cokit-rpc"))
            implementation(project(":cokit-protocol"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":cokit-testing"))
        }
        jvmTest.dependencies {
            implementation(project(":cokit-transport-stdio"))
        }
    }
}
