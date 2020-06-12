package io.github.lucaargolo.opticalnetworks.blocks.controller

import io.github.lucaargolo.opticalnetworks.blocks.CONTROLLER
import io.github.lucaargolo.opticalnetworks.network.NetworkState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class ControllerScreenHandler(syncId: Int, playerInventory: PlayerInventory, val network: NetworkState.Network, private val context: ScreenHandlerContext): ScreenHandler(null, syncId) {

    val invStack: DefaultedList<ItemStack> = DefaultedList.ofSize(1, ItemStack.EMPTY);
    val inventory = object: Inventory {
        override fun markDirty() {}
        override fun clear() = invStack.clear()
        override fun size() = invStack.size
        override fun isEmpty() = invStack.isEmpty()
        override fun setStack(slot: Int, stack: ItemStack?) = if (slot < invStack.size) invStack[slot] = stack else {}
        override fun getStack(slot: Int) = if (slot >= invStack.size) ItemStack.EMPTY else invStack[slot]
        override fun removeStack(slot: Int) = Inventories.removeStack(invStack, slot)
        override fun removeStack(slot: Int, amount: Int) = Inventories.splitStack(invStack, slot, amount)
        override fun canPlayerUse(player: PlayerEntity?) = true
    }

    init {
        val i = 18

        addSlot(Slot(inventory, 0, 17, 90))

        (0..2).forEach { n ->
            (0..8).forEach { m ->
                addSlot(Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 103 + n * 18 + i))
            }
        }

        (0..8).forEach { n ->
            addSlot(Slot(playerInventory, n,  8 + n * 18, 161 + i))
        }
    }

    override fun close(player: PlayerEntity?) {
        super.close(player)
        context.run { world: World, _: BlockPos ->
            dropInventory(player, world, inventory)
        }
    }

    override fun transferSlot(player: PlayerEntity?, index: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return context.run({ world: World, blockPos: BlockPos ->
            if (world.getBlockState(
                    blockPos
                ).block != CONTROLLER
            ) false else player.squaredDistanceTo(
                blockPos.x + .5,
                blockPos.y + .5,
                blockPos.z + .5
            ) < 64.0
        }, true)
    }

}