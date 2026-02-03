plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    api(project(":plugins:word-guesser-plugin:word-guesser-plugin-data"))
    compileOnly(project(":plugins:word-guesser-plugin:word-guesser-plugin-minestom"))
    compileOnly(project(":plugins:word-guesser-plugin:word-guesser-plugin-hytale"))
}