package io.github.lucaargolo.opticalnetworks.blocks.terminal

import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier

class TerminalConfig {

    enum class Size(var rows: Int, var x: Int, var y: Int) {
        SHORT(3, 193, 167),
        REGULAR(5, 193, 203),
        TALL(7, 193, 239),
        AUTOMATIC(0, 193, 0)
    }

    enum class Sort {
        NAME,
        QUANTITY,
        ID
        //INVTWEAKS
    }

    enum class SortDirection {
        ASCENDING,
        DESCENDING
    }

    var size = Size.SHORT
    var sort = Sort.NAME
    var sortDirection = SortDirection.ASCENDING

    fun toTag(tag: CompoundTag): CompoundTag {
        tag.putInt("terminalSize", Size.values().indexOf(size))
        tag.putInt("terminalSort", Sort.values().indexOf(sort))
        tag.putInt("terminalSortDirection", SortDirection.values().indexOf(sortDirection))
        return tag
    }

    fun fromTag(tag: CompoundTag) {
        size = Size.values()[tag.getInt("terminalSize")]
        sort = Sort.values()[tag.getInt("terminalSort")]
        sortDirection = SortDirection.values()[tag.getInt("terminalSortDirection")]
    }

}