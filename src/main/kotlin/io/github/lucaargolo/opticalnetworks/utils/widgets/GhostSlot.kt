package io.github.lucaargolo.opticalnetworks.utils.widgets

import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.collection.DefaultedList

class GhostSlot(val index: Int, var x: Int, var y: Int, val isQuantifiable: Boolean) {

    constructor(index: Int, x: Int, y: Int): this(index, x, y, false)

    interface IBlockEntity {
        val ghostInv: DefaultedList<ItemStack>
    }

    interface IScreenHandler {
        val ghostSlots: MutableList<GhostSlot>
        fun getGhostInv(): DefaultedList<ItemStack>
        fun getBlockEntity(): BlockEntity
    }

}