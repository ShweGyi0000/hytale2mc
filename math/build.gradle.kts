plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}
dependencies {
    api(libs.korlibs.math)
    api(libs.korlibs.math.core)
    api(libs.korlibs.math.vector)
    api(libs.colormath)
    api(libs.kotlinxSerialization)
}