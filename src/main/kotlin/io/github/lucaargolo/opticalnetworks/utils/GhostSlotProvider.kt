package io.github.lucaargolo.opticalnetworks.utils

import net.minecraft.item.ItemStack
import net.minecraft.util.collection.DefaultedList

interface GhostSlotProvider {

    val ghostInv: DefaultedList<ItemStack>

}