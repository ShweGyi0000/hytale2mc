package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm")
}

group = "com.hytale2mc"
version = "0.0.1"

kotlin {
    jvmToolchain(25)
}

tasks.withType<KotlinCompilationTask<KotlinCommonCompilerOptions>>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-Xcontext-sensitive-resolution",
            "-Xdata-flow-based-exhaustiveness",
            "-opt-in=kotlin.uuid.ExperimentalUuidApi"
        )
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
}
