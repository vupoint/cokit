import com.vanniktech.maven.publish.JavaPlatform

plugins {
    `java-platform`
    alias(libs.plugins.maven.publish)
}

dependencies {
    constraints {
        api(project(":cokit-protocol"))
        api(project(":cokit-rpc"))
        api(project(":cokit-client-api"))
        api(project(":cokit-client"))
        api(project(":cokit-transport-stdio"))
        api(project(":cokit-transport-websocket"))
        api(project(":cokit-testing"))
        api("${project.group}:cokit-protocol-jvm:${project.version}")
        api("${project.group}:cokit-rpc-jvm:${project.version}")
        api("${project.group}:cokit-client-api-jvm:${project.version}")
        api("${project.group}:cokit-client-jvm:${project.version}")
        api("${project.group}:cokit-transport-stdio-jvm:${project.version}")
        api("${project.group}:cokit-transport-websocket-jvm:${project.version}")
        api("${project.group}:cokit-testing-jvm:${project.version}")
    }
}

mavenPublishing {
    configure(JavaPlatform())

    pom {
        name.set("CoKit BOM")
        description.set("Bill of materials for aligning CoKit module versions.")
    }
}

tasks.named("check") {
    dependsOn(rootProject.tasks.named("checkCokitBomConstraints"))
}
