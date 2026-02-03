plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    compileOnly(libs.hytale)
    api(project(":ecs"))
}