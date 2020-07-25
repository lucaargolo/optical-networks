package io.github.lucaargolo.opticalnetworks.blocks.drive_rack

import io.github.lucaargolo.opticalnetworks.items.basic.ItemDrive
import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.Direction

class DriveRackBlockEntity(block: Block): NetworkBlockEntity(block), SidedInventory {

    var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(10, ItemStack.EMPTY)
    var priority = 0

    override fun size() = inventory.size

    override fun getAvailableSlots(side: Direction?): IntArray {
        return intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    }

    override fun isEmpty(): Boolean {
        val iterator = this.inventory.iterator()
        var itemStack: ItemStack
        do {
            if (iterator.hasNext())
                return true
            itemStack = iterator.next()
        } while(itemStack.isEmpty)
        return false
    }

    override fun getStack(slot: Int) = inventory[slot]

    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)

    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(this.inventory, slot)

    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) {
            stack.count = maxCountPerStack
        }
    }

    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?) = true

    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?) = (stack?.item is ItemDrive)

    override fun clear()  = inventory.clear()

    override fun canPlayerUse(player: PlayerEntity?): Boolean {
        return if (world!!.getBlockEntity(pos) != this) {
            false
        } else {
            player!!.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        tag.putInt("priority", priority)
        Inventories.toTag(tag, inventory)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        super.fromTag(state, tag)
        priority = tag.getInt("priority")
        Inventories.fromTag(tag, inventory)
    }
}