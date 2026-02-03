[![Discord](https://img.shields.io/discord/1463305432549425174?style=for-the-badge&logo=discord&logoColor=white&label=Discord&color=5865F2)](https://discord.gg/WYkEWU4h97)

# Showcase


https://github.com/user-attachments/assets/0dd1647d-c700-4dca-94e7-312144ce7544


You can find more video showcases at [hytale2mc.com](https://hytale2mc.com)

# Core Concepts

## ECS (Entity Component System)
If you don't know what ECS is, you can read about it [here](https://en.wikipedia.org/wiki/Entity_component_system).

hytale2mc uses a custom implementation of ECS which is similar to [Bevy](https://bevy.org/). If you are familiar with Bevy getting used to this one shouldn't be too hard,
although there are a lot of features either missing or implemented poorly.

## Platform
A platform is the environment where the games can on. Currently, the only supported platforms are Minecraft (Java) and Hytale.

## Minestom
[Minestom](https://minestom.net/) is an open-source, lightweight Minecraft server implementation. It is used as the Minecraft server backend.

## NATS
[NATS](https://nats.io/) is a messaging system that is used to communicate and synchronize state across multiple platforms.

## ECSPlugin
An `ECSPlugin` packages a plugin's data (components, events, entity types), systems, resources and the platform handlers that know how to spawn and render those types. Plugins are created with the `ecsPlugin(...)` function so the composer can discover and register serializers automatically.

## ECSStarter
`ECSStarter` is a class containing the data required to run your backend server, such as plugin registration and the NATS connection.

## Replay
All games are replayable by default. Replays are persisted using NATS JetStream. After finishing a game you can watch the replay by passing the `replayId` of that game into the `ECSStarter`.

# Quick Start

Clone the repository and change into the project folder:

```bash
git clone https://github.com/alskea/hytale2mc.git
cd hytale2mc
```

Start a local NATS server (the repository includes a minimal Compose file under `nats/`):

```bash
cd nats
docker-compose up -d
```

Build the project with the Gradle wrapper:

```bash
./gradlew build
```

Run the Minestom server:

```bash
./gradlew :games:aim-tranier:aim-trainer-minestom:run
```

Run the Hytale server by placing the game's `-hytale-all` jar into your Hytale server's `mods` directory and start the Hytale server.

Connect to the Minestom server from Minecraft and to the Hytale server from Hytale.


https://github.com/user-attachments/assets/9af7f0f8-1ff3-4e25-8e9f-67eb48b2bde0


# Adding Your Own Plugins and Games
Look at the existing source code in `plugins/` and `games/` directories and based on that implement your own.
You are encouraged to submit pull requests containing plugins and games you make so that they can serve as examples. 

# Closing Notes
A lot of stuff is not implemented. I was implementing stuff based on my needs for my Quake game.
Feel free to implement whatever you need while writing your own games. 

This is a project I've been working on for a while, and I'm pretty happy with how it turned out. Have fun!
