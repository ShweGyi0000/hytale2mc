plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    api(libs.nats)
    api(libs.kotlinxSerialization)
    api(project(":event-stream"))
}