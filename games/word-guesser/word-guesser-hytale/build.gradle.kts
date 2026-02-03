plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.shadow)
}

tasks.jar {
    enabled = false
}
dependencies {
    api(project(":ecs:ecs-platform-hytale"))
    api(project(":games:word-guesser"))
    api(project(":plugins:std-plugin:std-plugin-hytale"))
    api(project(":plugins:chat-plugin:chat-plugin-hytale"))
    api(project(":plugins:word-guesser-plugin:word-guesser-plugin-hytale"))
    compileOnly(libs.hytale)
}
