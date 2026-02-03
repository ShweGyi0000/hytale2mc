plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}
dependencies {
    api(project(":ecs"))
    api(project(":plugins:chat-plugin:chat-plugin-data"))
}