package io.github.lucaargolo.opticalnetworks.blocks.crafting

import io.github.lucaargolo.opticalnetworks.blocks.CRAFTING_COMPUTER
import io.github.lucaargolo.opticalnetworks.items.basic.CraftingMemory
import io.github.lucaargolo.opticalnetworks.items.basic.CraftingProcessingUnit
import io.github.lucaargolo.opticalnetworks.utils.BlockEntityInventory
import io.github.lucaargolo.opticalnetworks.utils.BlockEntityScreenHandler
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class CraftingComputerScreenHandler(syncId: Int, playerInventory: PlayerInventory, entity: CraftingComputerBlockEntity, context: ScreenHandlerContext): BlockEntityScreenHandler<CraftingComputerBlockEntity>(syncId, playerInventory, entity, context) {

    private var player: PlayerEntity = playerInventory.player

    var inventory = BlockEntityInventory(this, entity)

    init {
        checkSize(inventory, 4)
        inventory.onOpen(playerInventory.player)
        val i: Int = (3 - 4) * 18

        addSlot(object: Slot(inventory, 0, 22, 28) {
            override fun canInsert(stack: ItemStack) = stack.item is CraftingProcessingUnit
        })

        (1..3).forEach {
            addSlot(object: Slot(inventory, it, 17 + (it-1) * 18, 63) {
                override fun canInsert(stack: ItemStack) = stack.item is CraftingMemory
            })
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
                ).block != CRAFTING_COMPUTER
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
            if (invSlot < 4) {
                if (!insertItem(itemStack2, 4, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else if (!insertItem(itemStack2, 0, 4, false)) {
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