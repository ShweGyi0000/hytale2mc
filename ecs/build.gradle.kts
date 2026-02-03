plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    api(project(":math"))
    api(project(":event-stream:event-stream-nats"))
    api(kotlin("reflect"))
}