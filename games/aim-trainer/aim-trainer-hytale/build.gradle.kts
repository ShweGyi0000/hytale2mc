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
    api(project(":games:aim-trainer"))
    api(project(":plugins:aim-trainer-plugin:aim-trainer-plugin-hytale"))
    api(project(":plugins:std-plugin:std-plugin-hytale"))
    compileOnly(libs.hytale)
}
