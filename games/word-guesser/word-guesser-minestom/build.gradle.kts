plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.shadow)
    application
}

tasks.jar {
    enabled = false
}

dependencies {
    api(project(":ecs:ecs-platform-minestom"))
    api(project(":games:word-guesser"))
    api(project(":plugins:std-plugin:std-plugin-minestom"))
    api(project(":plugins:chat-plugin:chat-plugin-minestom"))
    api(project(":plugins:word-guesser-plugin:word-guesser-plugin-minestom"))
}

application {
    mainClass = "com.hytale2mc.game.wordguesser.WordGuesserMinestomKt"
}