package com.hytale2mc.ecs.data.component

import com.hytale2mc.ecs.text.Text
import kotlinx.serialization.Serializable

@Serializable
data class TabComponent(
    val header: Text? = null,
    val tab: Tab? = null,
    val footer: Text? = null
) : Component.Ecs2PlatformOnly() {

    @Serializable
    sealed interface Tab {

        @Serializable
        data class Grid(
            val columns: Int,
            val rows: List<Row>
        ) : Tab {

            @Serializable
            data class Row(val columns: List<Column>)

            @Serializable
            data class Column(val text: Text)

        }

    }

}