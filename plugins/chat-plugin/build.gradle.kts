plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    api(project(":plugins:std-plugin:std-plugin-data"))
    api(project(":plugins:chat-plugin:chat-plugin-data"))
    compileOnly(project(":plugins:chat-plugin:chat-plugin-minestom"))
    compileOnly(project(":plugins:chat-plugin:chat-plugin-hytale"))
}