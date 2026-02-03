plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":plugins:std-plugin"))
    api(project(":plugins:aim-trainer-plugin"))
}