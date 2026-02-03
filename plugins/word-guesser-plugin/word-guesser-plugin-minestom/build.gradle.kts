plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    api(project(":plugins:word-guesser-plugin:word-guesser-plugin-data"))
    compileOnly(project(":ecs:ecs-platform-minestom"))
}