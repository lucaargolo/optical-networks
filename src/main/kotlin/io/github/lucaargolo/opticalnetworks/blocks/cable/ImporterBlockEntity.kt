package io.github.lucaargolo.opticalnetworks.blocks.cable

import io.github.lucaargolo.opticalnetworks.blocks.cable.attachment.AttachmentBlockEntity
import io.github.lucaargolo.opticalnetworks.network.areStacksCompatible
import net.minecraft.block.Block
import net.minecraft.item.ItemStack

class ImporterBlockEntity(block: Block): AttachmentBlockEntity(block) {

    override fun tick() {
        super.tick()
        if (world?.isClient == false && delayCount >= 20 && currentNetwork != null) {
            val inventory = getAttachedInventory()
            if(inventory != null) {
                val slotMap = mutableMapOf<ItemStack, Int>()
                val availableStacks = mutableListOf<ItemStack>()
                (0 until inventory.size()).forEach {
                    if(!inventory.getStack(it).isEmpty) {
                        slotMap[inventory.getStack(it)] = it
                        availableStacks.add(inventory.getStack(it))
                    }
                }
                inserted = false
                if(listMode == List.WHITELIST) {
                    val sampleInv = getOrderedInv(getFilterInv())
                    sampleInv.forEachIndexed { _, filterStack ->
                        availableStacks.forEach { storedStack ->
                            var matches = true
                            if (filterStack.item != storedStack.item) matches = false
                            if (nbtMode == Nbt.MATCH && !ItemStack.areTagsEqual(filterStack, storedStack)) matches = false
                            if (damageMode == Damage.MATCH && !ItemStack.areItemsEqual(filterStack, storedStack)) matches = false
                            val matchedStack = if (matches) storedStack else ItemStack.EMPTY
                            if(!matchedStack.isEmpty) tryToImport(slotMap[matchedStack]!!, inventory, sampleInv.size)
                            if(inserted) return@forEachIndexed
                        }
                    }
                }else{
                    val sampleInv = getOrderedInv(availableStacks)
                    sampleInv.forEachIndexed { _, filterStack ->
                        var matches = false
                        getFilterInv().forEach { storedStack ->
                            if (areStacksCompatible(filterStack, storedStack)) matches = true
                            if (nbtMode == Nbt.IGNORE && ItemStack.areItemsEqual(filterStack, storedStack)) matches = true
                            if (damageMode == Damage.IGNORE && ItemStack.areItemsEqualIgnoreDamage(filterStack, storedStack)) matches = true
                        }
                        val matchedStack = if (!matches) filterStack else ItemStack.EMPTY
                        if(!matchedStack.isEmpty) tryToImport(slotMap[matchedStack]!!, inventory, sampleInv.size)
                        if(inserted) return@forEachIndexed
                    }
                }

            }
            delayCount = 0;
        }
        delayCount++
    }


}