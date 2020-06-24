package io.github.lucaargolo.opticalnetworks.blocks.terminal

import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import io.github.lucaargolo.opticalnetworks.utils.GhostSlot
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.collection.DefaultedList

class BlueprintTerminalBlockEntity(block: Block): NetworkBlockEntity(block), GhostSlot.GhostSlotBlockEntity, Inventory {

    override var ghostInv: DefaultedList<ItemStack> = DefaultedList.ofSize(9, ItemStack.EMPTY)
    var blueprintInv: DefaultedList<ItemStack> = DefaultedList.ofSize(1, ItemStack.EMPTY)

    override fun toTag(tag: CompoundTag): CompoundTag {
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
        ghostInv = DefaultedList.ofSize(9, ItemStack.EMPTY)
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
        blueprintInv[slot] = stack
        if (stack!!.count > maxCountPerStack) {
            stack.count = maxCountPerStack
        }
    }

    override fun clear()  = blueprintInv.clear()

    override fun canPlayerUse(player: PlayerEntity?): Boolean {
        return if (world!!.getBlockEntity(pos) != this) {
            false
        } else {
            player!!.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }
    }


}