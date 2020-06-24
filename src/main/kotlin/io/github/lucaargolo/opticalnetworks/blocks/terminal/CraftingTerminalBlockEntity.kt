package io.github.lucaargolo.opticalnetworks.blocks.terminal

import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
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

class CraftingTerminalBlockEntity(block: Block): NetworkBlockEntity(block), Inventory {

    var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(9, ItemStack.EMPTY)

    override fun toTag(tag: CompoundTag): CompoundTag {
        Inventories.toTag(tag, inventory)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        super.fromTag(state, tag)
        Inventories.fromTag(tag, inventory)
    }

    override fun size() = inventory.size

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

    override fun setStack(slot: Int, stack: ItemStack?) {
        inventory[slot] = stack
        if (stack!!.count > maxCountPerStack) {
            stack.count = maxCountPerStack
        }
    }

    override fun clear()  = inventory.clear()

    override fun canPlayerUse(player: PlayerEntity?): Boolean {
        return if (world!!.getBlockEntity(pos) != this) {
            false
        } else {
            player!!.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }
    }
}