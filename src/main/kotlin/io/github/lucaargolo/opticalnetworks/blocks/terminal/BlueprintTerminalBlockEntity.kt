package io.github.lucaargolo.opticalnetworks.blocks.terminal

import io.github.lucaargolo.opticalnetworks.items.blueprint.Blueprint
import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import io.github.lucaargolo.opticalnetworks.utils.widgets.GhostSlot
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.Direction

class BlueprintTerminalBlockEntity(block: Block): NetworkBlockEntity(block), GhostSlot.IBlockEntity, SidedInventory {

    override var ghostInv: DefaultedList<ItemStack> = DefaultedList.ofSize(12, ItemStack.EMPTY)
    var blueprintInv: DefaultedList<ItemStack> = DefaultedList.ofSize(1, ItemStack.EMPTY)
    var currentMode: Int = 0
    var useItemTag: Boolean = false
    var useNbtTag: Boolean = false

    override fun toTag(tag: CompoundTag): CompoundTag {
        tag.putBoolean("useItemTag", useItemTag)
        tag.putBoolean("useNbtTag", useNbtTag)
        tag.putInt("currentMode", currentMode)
        val ghostInvTag = CompoundTag()
        Inventories.toTag(ghostInvTag, ghostInv)
        tag.put("ghostInv", ghostInvTag)
        val blueprintInvTag = CompoundTag()
        Inventories.toTag(blueprintInvTag, blueprintInv)
        tag.put("blueprintInv", blueprintInvTag)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        super.fromTag(state, tag)
        useItemTag = tag.getBoolean("useItemTag")
        useNbtTag = tag.getBoolean("useNbtTag")
        currentMode = tag.getInt("currentMode")
        ghostInv = DefaultedList.ofSize(12, ItemStack.EMPTY)
        Inventories.fromTag(tag.getCompound("ghostInv"), ghostInv)
        blueprintInv = DefaultedList.ofSize(1, ItemStack.EMPTY)
        Inventories.fromTag(tag.getCompound("blueprintInv"), blueprintInv)
    }

    override fun size() = blueprintInv.size

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

    override fun setStack(slot: Int, stack: ItemStack?) {
        if(stack?.item !is Blueprint) return
        blueprintInv[slot] = stack
        if (stack.count > maxCountPerStack) {
            stack.count = maxCountPerStack
        }
    }

    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?) = false

    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?) = stack?.item is Blueprint

    override fun getAvailableSlots(side: Direction?): IntArray {
        return intArrayOf(0)
    }

    override fun clear() = blueprintInv.clear()

    override fun canPlayerUse(player: PlayerEntity?): Boolean {
        return if (world!!.getBlockEntity(pos) != this) {
            false
        } else {
            player!!.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }
    }


}