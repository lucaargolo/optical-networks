package io.github.lucaargolo.opticalnetworks.blocks.assembler

import io.github.lucaargolo.opticalnetworks.blocks.ASSEMBLER
import io.github.lucaargolo.opticalnetworks.items.blueprint.Blueprint
import io.github.lucaargolo.opticalnetworks.utils.BlockEntityInventory
import io.github.lucaargolo.opticalnetworks.utils.handlers.BlockEntityScreenHandler
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class AssemblerScreenHandler(syncId: Int, playerInventory: PlayerInventory, entity: AssemblerBlockEntity, context: ScreenHandlerContext): BlockEntityScreenHandler<AssemblerBlockEntity>(syncId, playerInventory, entity, context) {

    private var player: PlayerEntity = playerInventory.player

    var inventory: Inventory = BlockEntityInventory(this, entity)

    init {
        checkSize(inventory, 10)
        inventory.onOpen(playerInventory.player)
        val i: Int = (3 - 4) * 18

        addSlot(object: Slot(inventory, 0, 18, 35) {
            override fun canInsert(stack: ItemStack) = stack.item is Blueprint && stack.hasTag() && stack.tag!!.contains("type") && stack.tag!!.getString("type") == "crafting"
        })

        (0..2).forEach {n ->
            (0..2).forEach { m ->
                addSlot(Slot(inventory, (m + n * 3) + 1, 18 + (m+2) * 18, 17 + n * 18))
            }
        }

        addSlot(object: Slot(inventory, 10, 148, 35) {
            override fun canInsert(stack: ItemStack) = false
        })

        (0..2).forEach { n ->
            (0..8).forEach { m ->
                addSlot(Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 102 + n * 18 + i))
            }
        }

        (0..8).forEach { n ->
            addSlot(Slot(playerInventory, n, 8 + n * 18, 160 + i))
        }

    }


    override fun canUse(player: PlayerEntity): Boolean {
        return context.run({ world: World, blockPos: BlockPos ->
            if (world.getBlockState(
                    blockPos
                ).block != ASSEMBLER
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
            if (invSlot < 11) {
                if (!insertItem(itemStack2, 11, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else if (!insertItem(itemStack2, 1, 10, false)) {
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