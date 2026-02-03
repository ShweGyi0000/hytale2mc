import com.hytale2mc.ecs.data.EntityId
import com.hytale2mc.ecs.data.SoundData
import com.hytale2mc.ecs.platform.EntityProperties
import com.hytale2mc.ecs.platform.minestom.MinestomEntity
import com.hytale2mc.ecs.platform.minestom.MinestomItem
import com.hytale2mc.ecs.platform.minestom.MinestomPlatform
import com.hytale2mc.plugin.wordguesser.WordGuesserBlockType
import com.hytale2mc.plugin.wordguesser.WordGuesserEntityType
import com.hytale2mc.plugin.wordguesser.WordGuesserItemType
import com.hytale2mc.plugin.wordguesser.WordGuesserPlatformHandler
import com.hytale2mc.plugin.wordguesser.WordGuesserRenderable
import com.hytale2mc.plugin.wordguesser.WordGuesserSoundType
import korlibs.math.geom.Vector3I

object WordGuesserMinestom : WordGuesserPlatformHandler<MinestomEntity, MinestomItem, MinestomPlatform> {

    override fun init(platform: MinestomPlatform) {}

    override fun render(
        platform: MinestomPlatform,
        renderable: WordGuesserRenderable
    ) {
        throw IllegalStateException()
    }

    override fun spawn(
        platform: MinestomPlatform,
        entityId: EntityId,
        entityType: WordGuesserEntityType
    ): Pair<MinestomEntity, EntityProperties?> {
        throw IllegalStateException()
    }

    override fun setBlock(
        platform: MinestomPlatform,
        position: Vector3I,
        blockType: WordGuesserBlockType
    ) {
        when (blockType) {
            WordGuesserBlockType.Floor -> platform.instance.setBlock(position.x, position.y, position.z, STONE)
        }
    }

    override fun createItem(
        platform: MinestomPlatform,
        itemType: WordGuesserItemType
    ): MinestomItem {
        throw IllegalStateException()
    }

    override fun getSoundData(soundType: WordGuesserSoundType): List<SoundData> {
        throw IllegalStateException()
    }

    override fun onRemove(platformEntity: MinestomEntity) {}
}