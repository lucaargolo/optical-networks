package io.github.lucaargolo.opticalnetworks.utils

import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.collection.DefaultedList

class GhostSlot(val index: Int, var x: Int, var y: Int) {

    interface GhostSlotBlockEntity {
        val ghostInv: DefaultedList<ItemStack>
    }

    interface GhostSlotScreenHandler {
        val ghostSlots: MutableList<GhostSlot>
        fun getGhostInv(): DefaultedList<ItemStack>
        fun getBlockEntity(): BlockEntity
    }

}
