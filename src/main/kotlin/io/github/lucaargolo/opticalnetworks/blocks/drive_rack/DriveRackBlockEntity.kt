package io.github.lucaargolo.opticalnetworks.blocks.drive_rack

import io.github.lucaargolo.opticalnetworks.blocks.getEntityType
import io.github.lucaargolo.opticalnetworks.items.basic.DiscDrive
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.collection.DefaultedList

class DriveRackBlockEntity(val block: Block): LockableContainerBlockEntity(getEntityType(block)), BlockEntityClientSerializable {

    var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(10, ItemStack.EMPTY)
    var priority = 0;

    override fun createScreenHandler(syncId: Int, playerInventory: PlayerInventory?) = null

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

    override fun setStack(slot: Int, stack: ItemStack) {
        if(stack.item !is DiscDrive) return
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) {
            stack.count = maxCountPerStack
        }
    }

    override fun clear()  = inventory.clear()

    override fun getContainerName(): Text = TranslatableText("Drive Rack")

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

    override fun toClientTag(tag: CompoundTag): CompoundTag {
        return toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        super.fromTag(state, tag)
        priority = tag.getInt("priority")
        Inventories.fromTag(tag, inventory)
    }

    override fun fromClientTag(tag: CompoundTag) {
        fromTag(block.defaultState, tag)
    }
}