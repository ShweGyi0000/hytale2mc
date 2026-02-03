package com.hytale2mc.plugin.wordguesser

import com.hytale2mc.ecs.data.Resource
import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.data.state.State
import com.hytale2mc.ecs.data.time.Timer
import com.hytale2mc.ecs.data.time.TimerMode
import com.hytale2mc.ecs.plugin.PluginKey
import com.hytale2mc.ecs.plugin.PluginPlatformHandler
import korlibs.time.seconds
import kotlinx.serialization.Serializable

typealias WordGuesserPlatformHandler<P_E, P_I, P> = PluginPlatformHandler<WordGuesserKey, P_E, P_I, P, WordGuesserRenderable, WordGuesserEntityType, WordGuesserBlockType, WordGuesserItemType, WordGuesserSoundType>
@Serializable
data object WordGuesserKey : PluginKey<WordGuesserKey>

@Serializable
sealed interface WordGuesserBlockType : ECSDynamic.BlockType<WordGuesserKey> {

    override val owner: WordGuesserKey
        get() = WordGuesserKey

    @Serializable
    data object Floor : WordGuesserBlockType

}
@Serializable
sealed interface WordGuesserEntityType : ECSDynamic.EntityType<WordGuesserKey>
@Serializable
sealed interface WordGuesserItemType : ECSDynamic.ItemType<WordGuesserKey>
@Serializable
sealed interface WordGuesserRenderable : ECSDynamic.Renderable<WordGuesserKey>
@Serializable
sealed interface WordGuesserSoundType : ECSDynamic.SoundType<WordGuesserKey>

sealed interface WordGuesserState : State {

    data object Waiting : WordGuesserState

    data class Guessing(
        val word: Word,
    ) : WordGuesserState

}

data class WordTimer(
    val timer: Timer = Timer(duration = 5.seconds, mode = TimerMode.ONCE),
    val hintTimer: Timer = Timer(duration = 10.seconds, mode = TimerMode.REPEATING),
    val hintIndex: Int = 0
) : Resource()

data class Word(
    val word: String,
    val hints: List<String>
)

internal val WORDS = listOf(
    Word(
        "Camera",
        listOf(
            "I have a memory but no brain.",
            "I capture a moment but never live in it.",
            "I have a 'flash' but I am not a superhero.",
            "I have a shutter, but I am not a window.",
            "I use a lens to see the world in a single click."
        )
    ),
    Word(
        "Mirror",
        listOf(
            "I look at you, but I never speak first.",
            "I am the only thing that can show you your own face.",
            "If you smile, I smile back.",
            "I flip your right into your left.",
            "I am made of glass and backed by silver."
        )
    ),
    Word(
        "Rain",
        listOf(
            "I come from the sky but I am not a bird.",
            "I make things grow, but I can also wash them away.",
            "I create music when I hit a tin roof.",
            "I am the enemy of the picnic and the friend of the umbrella.",
            "I fall down, but I never get back up."
        )
    ),
    Word(
        "Piano",
        listOf(
            "I have many 'voices' but I cannot speak.",
            "I have a bench, but I am not in a park.",
            "I am made of wood and metal, but I have 88 keys.",
            "My keys are black and white, but I am not a zebra.",
            "You play me by pressing my keys with your fingers."
        )
    ),
    Word(
        "Library",
        listOf(
            "I contain thousands of voices, yet I am very quiet.",
            "I have many 'leaves' but I am not a tree.",
            "People visit me to travel to other worlds without leaving their seats.",
            "I am organized by the Dewey Decimal System.",
            "I am a building full of books you can borrow."
        )
    ),
    Word(
        "Candle",
        listOf(
            "I am tall when I am young and short when I am old.",
            "I cry tears of wax as I live.",
            "I give light, but only by consuming myself.",
            "A single breath is enough to end my life.",
            "I have a wick in the center that you light with a flame."
        )
    ),
    Word(
        "Compass",
        listOf(
            "I have a needle but I cannot sew.",
            "I always know where I am going, but I never go there.",
            "I am sensitive to the Earth's invisible pull.",
            "I have four main faces: North, South, East, and West.",
            "Sailors and hikers use me so they don't get lost."
        )
    ),
    Word(
        "Clock",
        listOf(
            "I have a face but no eyes.",
            "I have hands, but I cannot clap or hold anything.",
            "I tell you the truth, even if you don't want to hear it.",
            "My favorite sound is a steady 'tick-tock'.",
            "I measure the passing of seconds, minutes, and hours."
        )
    ),
    Word(
        "Volcano",
        listOf(
            "I am a mountain with a dangerous temper.",
            "I have a mouth but I do not eat.",
            "When I get angry, I spit fire and ash.",
            "I am often found in the 'Ring of Fire'.",
            "I erupt with molten lava."
        )
    ),
    Word(
        "Letter",
        listOf(
            "I am a traveler who stays in a corner.",
            "I carry secrets and news across oceans.",
            "I must be 'licked' before I can start my journey.",
            "I wear a 'stamp' as my passport.",
            "You put me in an envelope and drop me in a mailbox."
        )
    )
)
