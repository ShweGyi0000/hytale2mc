package com.hytale2mc.plugin.wordguesser

import com.github.ajalt.colormath.model.RGB
import com.hytale2mc.ecs.composer.SpaceComposer
import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.component.EntityTypeComponent
import com.hytale2mc.ecs.data.component.GravityComponent
import com.hytale2mc.ecs.data.component.InputComponent
import com.hytale2mc.ecs.data.component.NametagComponent
import com.hytale2mc.ecs.data.component.TransformComponent
import com.hytale2mc.ecs.data.component.Visibility
import com.hytale2mc.ecs.data.component.VisibilityComponent
import com.hytale2mc.ecs.data.event.PlayerMessageEvent
import com.hytale2mc.ecs.data.event.consume
import com.hytale2mc.ecs.data.operation.RenderingCommand
import com.hytale2mc.ecs.data.time.Time
import com.hytale2mc.ecs.data.time.Timer
import com.hytale2mc.ecs.phase.Phase
import com.hytale2mc.ecs.platform.shortenedWithColor
import com.hytale2mc.ecs.query.ComponentFilter
import com.hytale2mc.ecs.query.ComponentFilters
import com.hytale2mc.ecs.query.Query
import com.hytale2mc.ecs.query.get
import com.hytale2mc.ecs.query.getValue
import com.hytale2mc.ecs.query.setValue
import com.hytale2mc.ecs.system.exec
import com.hytale2mc.ecs.system.queueCommand
import com.hytale2mc.ecs.text.TextPart
import com.hytale2mc.plugin.chat.ChatRenderable
import com.hytale2mc.plugin.chat.ChatRenderable.ChatMessage.Companion.Everyone
import com.hytale2mc.plugin.chat.SendableMessage
import korlibs.math.geom.Vector3F
import korlibs.time.minutes
import korlibs.time.seconds

fun SpaceComposer.wordGuesserSystems() {
    playerInit { joined ->
        listOf(
            NametagComponent(
                listOf(
                    joined.platform.platformType.shortenedWithColor(),
                    TextPart(" ${joined.username}", color = RGB(1f, 1f, 1f))
                )
            ),
            TransformComponent(
                translation = Vector3F(0f, 150f, 0f)
            ),
            VisibilityComponent(Visibility.ToEveryone),
            GravityComponent(),
            InputComponent()
        )
    }
    systems {
        system(
            "word_guesser:tick_state",
            Phase.Before,
            tickState
        )

        system(
            "word_guesser:on_enter_guessing",
            Phase.StateTransitions.Enter(WordGuesserState.Guessing::class),
            onEnterGuessing
        )

        system(
            "word_guesser:on_enter_waiting",
            Phase.StateTransitions.Enter(WordGuesserState.Waiting::class),
            onEnterWaiting
        )

        system(
            "word_guesser:check_guesses",
            Phase.Update,
            checkGuesses()
        )
    }
}

private val tickState = exec(
    object : Query() {
        var state by write<WordGuesserState>()
        var timer by write<WordTimer>()
        val time by read<Time>()
    },
    object : Query() {
        override val componentFilter: ComponentFilter = ComponentFilters.with<EntityTypeComponent>()
    }
) { q, entities ->
    when (val state = q.state) {
        WordGuesserState.Waiting -> {
            when {
                entities.entities.isEmpty() -> {
                    q.timer = q.timer.copy(timer = q.timer.timer.reset())
                }
                q.timer.timer.isFinished -> {
                    q.state = WordGuesserState.Guessing(WORDS[space.random.nextInt(0, WORDS.size)])
                }
            }
        }
        is WordGuesserState.Guessing -> {
            when {
                q.timer.timer.isFinished -> {
                    queueCommand(
                        RenderingCommand.Render(
                            "word_guesser:no_one_guessed",
                            ChatRenderable.ChatMessage(
                                SendableMessage.Chat(
                                    listOf(TextPart("No one guessed the word, it was: \"${state.word.word}\""))
                                ),
                                receivers = Everyone
                            )
                        )
                    )
                    q.state = WordGuesserState.Waiting
                }
                q.timer.hintTimer.justFinished -> {
                    q.timer = q.timer.copy(hintIndex = q.timer.hintIndex + 1)
                    queueCommand(
                        RenderingCommand.Render(
                            "word_guesser:reveal_hint",
                            ChatRenderable.ChatMessage(
                                SendableMessage.Chat(
                                    listOf(TextPart("\n== Hints ==\n")) +
                                            state.word.hints.mapIndexed { i, hint ->
                                                val hint = if (q.timer.hintIndex > i) {
                                                    hint
                                                } else {
                                                    "???"
                                                }
                                                TextPart("${i + 1}. $hint\n")
                                            }
                                ),
                                receivers = Everyone
                            )
                        )
                    )
                }
            }
        }
    }
    q.timer = q.timer.copy(
        timer = q.timer.timer.tick(q.time.delta),
        hintTimer = q.timer.hintTimer.tick(q.time.delta)
    )
}

val onEnterGuessing = exec(
    object : Query() {
        var timer by write<WordTimer>()
    }
) { q ->
    q.timer = q.timer.copy(timer = Timer(1.minutes), hintTimer = q.timer.hintTimer.finish())
    queueCommand(
        RenderingCommand.Render(
            "word_guesser:notify_new_word",
            listOf(
                ChatRenderable.ChatMessage(
                    SendableMessage.Chat(
                        listOf(TextPart("type a message in chat to guess the word"))
                    ),
                    receivers = Everyone
                )
            )
        )
    )
}

val onEnterWaiting = exec(
    object : Query() {
        var timer by write<WordTimer>()
    }
) { q ->
    q.timer = q.timer.copy(timer = Timer(5.seconds), hintTimer = q.timer.hintTimer.reset(), hintIndex = 0)
}

fun SpaceComposer.checkGuesses() = exec(
    object : Query() {
        val nametag = read<NametagComponent>()
    },
    object : Query() {
        val guess = readEvent<PlayerMessageEvent>()
        var state by write<WordGuesserState>()
        val timer by read<WordTimer>()
    }
) { q, guesses ->
    val state = guesses.state
    if (state !is WordGuesserState.Guessing) {
        guesses.guess.consume()
        return@exec
    }
    var guessedCorrectly: EntityId? = null
    for (g in guesses.guess.consume()) {
        if (g.message.lowercase() != state.word.word.lowercase()) {
            continue
        }
        if (g.sender !in q.entities) {
            continue
        }
        guessedCorrectly = g.sender
        continue
    }
    if (guessedCorrectly != null) {
        queueCommand(RenderingCommand.Render(
            "word_guesser:guessed",
            ChatRenderable.ChatMessage(
                SendableMessage.Chat(
                    q.nametag[guessedCorrectly].text
                            + listOf(TextPart(" guessed the word correctly in ${guesses.timer.timer.elapsed()}. The word was \"${state.word.word}\"", color = RGB(1f, 1f, 1f)))
                ),
                receivers = Everyone
            )
        ))
        guesses.state = WordGuesserState.Waiting
    }
}