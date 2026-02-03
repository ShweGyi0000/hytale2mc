plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    api(project(":plugins:aim-trainer-plugin:aim-trainer-plugin-data"))
    compileOnly(project(":plugins:aim-trainer-plugin:aim-trainer-plugin-minestom"))
    compileOnly(project(":plugins:aim-trainer-plugin:aim-trainer-plugin-hytale"))
}