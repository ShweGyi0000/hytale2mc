dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven("https://maven.hytale.com/release")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "hytale2mc"

include(":math")

include(":event-stream")
include(":event-stream:event-stream-nats")

include(":ecs")

include(":ecs:ecs-platform-minestom")
include(":ecs:ecs-platform-hytale")

include(":games:aim-trainer")
include(":games:aim-trainer:aim-trainer-minestom")
include(":games:aim-trainer:aim-trainer-hytale")

include(":games:word-guesser")
include(":games:word-guesser:word-guesser-minestom")
include(":games:word-guesser:word-guesser-hytale")

include(":plugins:std-plugin")
include(":plugins:std-plugin:std-plugin-data")
include(":plugins:std-plugin:std-plugin-minestom")
include(":plugins:std-plugin:std-plugin-hytale")

include(":plugins:chat-plugin")
include(":plugins:chat-plugin:chat-plugin-data")
include(":plugins:chat-plugin:chat-plugin-minestom")
include(":plugins:chat-plugin:chat-plugin-hytale")

include(":plugins:word-guesser-plugin")
include(":plugins:word-guesser-plugin:word-guesser-plugin-data")
include(":plugins:word-guesser-plugin:word-guesser-plugin-minestom")
include(":plugins:word-guesser-plugin:word-guesser-plugin-hytale")

include(":plugins:aim-trainer-plugin")
include(":plugins:aim-trainer-plugin:aim-trainer-plugin-data")
include(":plugins:aim-trainer-plugin:aim-trainer-plugin-minestom")
include(":plugins:aim-trainer-plugin:aim-trainer-plugin-hytale")
