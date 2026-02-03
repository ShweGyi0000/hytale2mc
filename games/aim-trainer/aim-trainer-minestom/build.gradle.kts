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
    api(project(":games:aim-trainer"))
    api(project(":plugins:aim-trainer-plugin:aim-trainer-plugin-minestom"))
    api(project(":plugins:std-plugin:std-plugin-minestom"))
}

application {
    mainClass = "com.hytale2mc.game.aimtrainer.AimTrainerMinestomKt"
}