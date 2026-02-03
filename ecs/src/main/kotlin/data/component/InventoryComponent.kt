package com.hytale2mc.ecs.data.component

import com.hytale2mc.ecs.data.dynamic.ECSDynamic
import com.hytale2mc.ecs.text.Text
import kotlinx.serialization.Serializable

@Serializable
data class InventoryComponent(
    val slots: List<InventoryItem?> = createEmptySlots(),
) : Component.Ecs2PlatformOnly() {

    companion object {

        fun createEmptySlots(): MutableList<InventoryItem?> {
            return MutableList(9) { null }
        }

    }

}


@Serializable
data class InventoryItem(
    val itemType: ECSDynamic.ItemType<*>,
    val amount: Int,
    val textInInventory: Text?
)