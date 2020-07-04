package io.github.lucaargolo.opticalnetworks.blocks.cable

import io.github.lucaargolo.opticalnetworks.blocks.attachment.AttachmentBlockEntity
import io.github.lucaargolo.opticalnetworks.utils.areStacksCompatible
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.state.property.Properties

class ExporterBlockEntity(block: Block): AttachmentBlockEntity(block) {

    override fun tick() {
        super.tick()
        if (world?.isClient == false && delayCount >= 20 && currentNetwork != null) {
            val inventory = getAttachedInventory()
            if(inventory != null) {
                val availableStacks = currentNetwork!!.getAvailableStacks("")
                inserted = false
                if(listMode == List.WHITELIST) {
                    val sampleInv = getOrderedInv(getFilterInv())
                    sampleInv.forEachIndexed { sortIndex, filterStack ->
                        availableStacks.forEach { storedStack ->
                            var matches = true
                            if (filterStack.item != storedStack.item) matches = false
                            if (nbtMode == Nbt.MATCH && !ItemStack.areTagsEqual(filterStack, storedStack)) matches = false
                            if (damageMode == Damage.MATCH && !ItemStack.areItemsEqual(filterStack, storedStack)) matches = false
                            val matchedStack = if (matches) filterStack.copy() else ItemStack.EMPTY
                            if(!matchedStack.isEmpty) tryToExport(matchedStack, inventory, sampleInv.size, cachedState[Properties.FACING].opposite)
                            if(inserted) return@forEachIndexed
                        }
                    }
                }else{
                    val sampleInv = getOrderedInv(availableStacks)
                    sampleInv.forEachIndexed { sortIndex, filterStack ->
                        var matches = false
                        getFilterInv().forEach { storedStack ->
                            if (areStacksCompatible(filterStack, storedStack)) matches = true
                            if (nbtMode == Nbt.IGNORE && ItemStack.areItemsEqual(filterStack, storedStack)) matches = true
                            if (damageMode == Damage.IGNORE && ItemStack.areItemsEqualIgnoreDamage(filterStack, storedStack)) matches = true
                        }
                        val matchedStack = if (!matches) filterStack.copy() else ItemStack.EMPTY
                        if(!matchedStack.isEmpty) tryToExport(matchedStack, inventory, sampleInv.size, cachedState[Properties.FACING].opposite)
                        if(inserted) return@forEachIndexed
                    }
                }

            }
            delayCount = 0;
        }
        delayCount++
    }

}