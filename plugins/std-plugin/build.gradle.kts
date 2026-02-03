plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    api(project(":plugins:std-plugin:std-plugin-data"))
    compileOnly(project(":plugins:std-plugin:std-plugin-minestom"))
    compileOnly(project(":plugins:std-plugin:std-plugin-hytale"))
}