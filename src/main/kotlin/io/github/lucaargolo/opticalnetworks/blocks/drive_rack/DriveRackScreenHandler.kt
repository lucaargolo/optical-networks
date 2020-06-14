package io.github.lucaargolo.opticalnetworks.blocks.drive_rack

import io.github.lucaargolo.opticalnetworks.blocks.DRIVE_RACK
import io.github.lucaargolo.opticalnetworks.items.basic.DiscDrive
import io.github.lucaargolo.opticalnetworks.utils.BlockEntityScreenHandler
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class DriveRackScreenHandler (syncId: Int, playerInventory: PlayerInventory, entity: DriveRackBlockEntity, context: ScreenHandlerContext): BlockEntityScreenHandler<DriveRackBlockEntity>(syncId, playerInventory, entity, context) {

    private var player: PlayerEntity = playerInventory.player

    var inventory: Inventory = object: Inventory {
        override fun size(): Int {
            return entity.size()
        }

        override fun isEmpty(): Boolean {
            return entity.isEmpty
        }

        override fun getStack(slot: Int): ItemStack? {
            return entity.getStack(slot)
        }

        override fun removeStack(slot: Int): ItemStack? {
            val stack: ItemStack = entity.removeStack(slot)
            onContentChanged(this)
            return stack
        }

        override fun removeStack(slot: Int, amount: Int): ItemStack? {
            val stack: ItemStack = entity.removeStack(slot, amount)
            onContentChanged(this)
            return stack
        }

        override fun setStack(slot: Int, stack: ItemStack) {
            entity.setStack(slot, stack)
            onContentChanged(this)
        }

        override fun markDirty() {
            entity.markDirty()
        }

        override fun canPlayerUse(player: PlayerEntity?): Boolean {
            return entity.canPlayerUse(player)
        }

        override fun clear() {
            entity.clear()
        }

    }

    init {
        checkSize(inventory, 10)
        inventory.onOpen(playerInventory.player)
        val i: Int = (3 - 4) * 18

        (0..4).forEach {n ->
            (0..1).forEach { m ->
                addSlot(object: Slot(inventory, m + n * 2, 35 + (m+2) * 18, n * 18) {
                    override fun canInsert(stack: ItemStack): Boolean {
                        return stack.item is DiscDrive
                    }
                })
            }
        }

        (0..2).forEach { n ->
            (0..8).forEach { m ->
                addSlot(Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 121 + n * 18 + i))
            }
        }

        (0..8).forEach { n ->
            addSlot(Slot(playerInventory, n, 8 + n * 18, 179 + i))
        }

    }


    override fun canUse(player: PlayerEntity): Boolean {
        return context.run({ world: World, blockPos: BlockPos ->
            if (world.getBlockState(
                    blockPos
                ).block != DRIVE_RACK
            ) false else player.squaredDistanceTo(
                blockPos.x + .5,
                blockPos.y + .5,
                blockPos.z + .5
            ) < 64.0
        }, true)
    }

    override fun transferSlot(player: PlayerEntity, invSlot: Int): ItemStack? {
        var itemStack = ItemStack.EMPTY
        val slot = slots[invSlot]
        if (slot != null && slot.hasStack()) {
            val itemStack2 = slot.stack
            itemStack = itemStack2.copy()
            if (invSlot < 10) {
                if (!insertItem(itemStack2, 10, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else if (!insertItem(itemStack2, 0, 10, false)) {
                return ItemStack.EMPTY
            }
            if (itemStack2.isEmpty) {
                slot.stack = ItemStack.EMPTY
            } else {
                slot.markDirty()
            }
        }

        return itemStack
    }

}