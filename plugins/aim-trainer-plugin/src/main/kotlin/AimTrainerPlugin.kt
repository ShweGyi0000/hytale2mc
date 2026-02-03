package com.hytale2mc.plugin.aimtrainer

import com.hytale2mc.ecs.data.ECSBlock
import com.hytale2mc.ecs.data.ECSMap
import com.hytale2mc.ecs.plugin.ecsPlugin
import com.hytale2mc.plugin.aimtrainer.hytale.AimTrainerHytale
import com.hytale2mc.plugin.aimtrainer.minestom.AimTrainerMinestom
import korlibs.math.geom.Vector3I

val aimTrainerPlugin = ecsPlugin<AimTrainerKey, AimTrainerRenderable, AimTrainerEntityType, AimTrainerBlockType, AimTrainerItemType, AimTrainerSoundType>(
    AimTrainerKey,
    handlers = {
        minecraft { AimTrainerMinestom }
        hytale { AimTrainerHytale }
    },
    components = {
        component<DeadComponent>()
        component<TargetComponent>()
    },
    maps = {
        preBuilt(
            "aim-trainer",
            ECSMap(
                buildList {
                    for (z in gridMin.z.toInt() - 2..gridMax.z.toInt() + 2) {
                        for (y in gridMin.y.toInt() - 2..gridMax.y.toInt() + 2) {
                            add(ECSBlock(Vector3I(gridMax.x.toInt(), y, z), AimTrainerBlockType.Wall))
                        }
                    }
                    for (x in -10..10) {
                        for (z in -10..10) {
                            add(ECSBlock(Vector3I(x, floorY, z), AimTrainerBlockType.Floor))
                        }
                    }
                }
            )
        )
    },
    systems = {
        aimTrainerSystems()
    }
)