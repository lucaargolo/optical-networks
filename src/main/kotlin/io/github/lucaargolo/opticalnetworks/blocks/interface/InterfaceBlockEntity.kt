package io.github.lucaargolo.opticalnetworks.blocks.`interface`

import io.github.lucaargolo.opticalnetworks.blocks.attachment.AttachmentBlockEntity
import io.github.lucaargolo.opticalnetworks.items.blueprint.Blueprint
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.Direction

class InterfaceBlockEntity(block: Block): AttachmentBlockEntity(block), SidedInventory {

    override var ghostInvSize = 18
    override var ghostInv: DefaultedList<ItemStack> = DefaultedList.ofSize(18, ItemStack.EMPTY)
    var blueprintInv: DefaultedList<ItemStack> = DefaultedList.ofSize(9, ItemStack.EMPTY)

    override fun size() = blueprintInv.size

    override fun getAvailableSlots(side: Direction?): IntArray {
        return intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 9)
    }

    override fun isEmpty(): Boolean {
        val iterator = this.blueprintInv.iterator()
        var itemStack: ItemStack
        do {
            if (iterator.hasNext())
                return true
            itemStack = iterator.next()
        } while(itemStack.isEmpty)
        return false
    }

    override fun getStack(slot: Int) = blueprintInv[slot]

    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(blueprintInv, slot, amount)

    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(this.blueprintInv, slot)

    override fun setStack(slot: Int, stack: ItemStack) {
        blueprintInv[slot] = stack
        if (stack.count > maxCountPerStack) {
            stack.count = maxCountPerStack
        }
    }

    override fun canExtract(slot: Int, stack: ItemStack, dir: Direction?) = true

    override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?) = (stack.item is Blueprint && stack.hasTag() && stack.tag!!.contains("type"))

    override fun clear()  = blueprintInv.clear()

    override fun canPlayerUse(player: PlayerEntity?): Boolean {
        return if (world!!.getBlockEntity(pos) != this) {
            false
        } else {
            player!!.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        val blueprintTag = CompoundTag()
        Inventories.toTag(blueprintTag, blueprintInv)
        tag.put("blueprintInv", blueprintTag)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        super.fromTag(state, tag)
        Inventories.fromTag(tag.getCompound("blueprintInv"), blueprintInv)
    }

}