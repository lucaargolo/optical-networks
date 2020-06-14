package io.github.lucaargolo.opticalnetworks.blocks.cable.exporter

import io.github.lucaargolo.opticalnetworks.network.areStacksCompatible
import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import io.github.lucaargolo.opticalnetworks.utils.GhostSlotProvider
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ChestBlock
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.state.property.Properties
import net.minecraft.util.collection.DefaultedList

class ExporterBlockEntity(block: Block): NetworkBlockEntity(block), GhostSlotProvider {
    enum class List {
        WHITELIST,
        BLACKLIST
    }

    enum class Nbt {
        MATCH,
        IGNORE
    }

    enum class Damage {
        MATCH,
        IGNORE
    }

    enum class Order {
        FIRST_TO_LAST,
        LAST_TO_FIRST,
        ROUND_ROBIN,
        RANDOM
    }

    enum class Redstone {
        IGNORE,
        NEEDS_ON,
        NEEDS_OFF
    }

    override var ghostInv: DefaultedList<ItemStack> = DefaultedList.ofSize(9, ItemStack.EMPTY)
    var listMode = List.WHITELIST
    var nbtMode = Nbt.MATCH
    var damageMode = Damage.IGNORE
    var orderMode = Order.FIRST_TO_LAST
    var redstoneMode = Redstone.IGNORE
    var delayCount = 0
    var lastInsert = 0

    override fun toTag(tag: CompoundTag): CompoundTag {
        Inventories.toTag(tag, ghostInv)
        tag.putInt("listMode", List.values().indexOf(listMode))
        tag.putInt("nbtMode", Nbt.values().indexOf(nbtMode))
        tag.putInt("damageMode", Damage.values().indexOf(damageMode))
        tag.putInt("orderMode", Order.values().indexOf(orderMode))
        tag.putInt("redstoneMode", Order.values().indexOf(redstoneMode))
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        super.fromTag(state, tag)
        ghostInv = DefaultedList.ofSize(9, ItemStack.EMPTY)
        Inventories.fromTag(tag, ghostInv)
        listMode = List.values()[tag.getInt("listMode")]
        nbtMode = Nbt.values()[tag.getInt("nbtMode")]
        damageMode = Damage.values()[tag.getInt("damageMode")]
        orderMode = Order.values()[tag.getInt("orderMode")]
        redstoneMode = Redstone.values()[tag.getInt("redstoneMode")]
    }

    var inserted = false

    override fun tick() {
        super.tick()
        if (world?.isClient == false && delayCount >= 20 && currentNetwork != null) {
            val inventory = getOutputInventory()
            if(inventory != null) {
                val availableStacks = currentNetwork!!.searchStacks("")
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
                            if(!matchedStack.isEmpty) tryToInsert(matchedStack, inventory, sampleInv.size)
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
                        if(!matchedStack.isEmpty) tryToInsert(matchedStack, inventory, sampleInv.size)
                        if(inserted) return@forEachIndexed
                    }
                }

            }
            delayCount = 0;
        }
        delayCount++
    }

    private fun getFilterInv(): kotlin.collections.List<ItemStack> {
        val filterInv = mutableListOf<ItemStack>()
        ghostInv.forEach { if(!it.isEmpty) filterInv.add(it) }
        return filterInv
    }

    private fun getOrderedInv(list: kotlin.collections.List<ItemStack>): kotlin.collections.List<ItemStack> {
        if(lastInsert+1 > list.size) lastInsert = 0;
        return when(orderMode) {
            Order.LAST_TO_FIRST -> list.reversed()
            Order.RANDOM -> list.shuffled()
            Order.ROUND_ROBIN -> {
                val nl = mutableListOf<ItemStack>()
                nl.addAll(list.subList(lastInsert+1, list.size))
                nl.addAll(list.subList(0, lastInsert+1))
                nl
            }
            else -> list
        }
    }

    private fun tryToInsert(stack: ItemStack, inventory: Inventory, sampleSize: Int) {
        stack.count = 1
        val i = inventory.size()
        (0 until i).forEach {
            val stk = inventory.getStack(it)
            if (!inserted && stk.isEmpty) {
                inventory.setStack(it, stack.copy())
                currentNetwork!!.removeStack(stack)
                lastInsert = if(lastInsert+1 == sampleSize) 0 else if(lastInsert+1 > sampleSize) lastInsert+1-sampleSize else lastInsert+1
                println(lastInsert)
                inserted = true
            } else if (!inserted && stk.count < stk.maxCount && areStacksCompatible(stack, stk)) {
                stk.increment(1)
                currentNetwork!!.removeStack(stack)
                lastInsert = if(lastInsert+1 == sampleSize) 0 else if(lastInsert+1 > sampleSize) lastInsert+1-sampleSize else lastInsert+1
                println(lastInsert)
                inserted = true
            }
        }
    }

    private fun getOutputInventory(): Inventory? {
        var inventory: Inventory? = null
        val world = world!!
        val facing = cachedState[Properties.FACING]
        val blockPos = pos.add(facing.vector)
        val blockState = world.getBlockState(blockPos)
        val block = blockState.block
        if (block is InventoryProvider) {
            inventory = (block as InventoryProvider).getInventory(blockState, world, blockPos)
        } else if (block.hasBlockEntity()) {
            val blockEntity = world.getBlockEntity(blockPos)
            if (blockEntity is Inventory) {
                inventory = blockEntity
                if (inventory is ChestBlockEntity && block is ChestBlock) {
                    inventory = ChestBlock.getInventory(block, blockState, world, blockPos, true)
                }
            }
        }
        return inventory
    }

}