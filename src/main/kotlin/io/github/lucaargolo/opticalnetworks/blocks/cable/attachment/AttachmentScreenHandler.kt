package io.github.lucaargolo.opticalnetworks.blocks.cable.attachment

import io.github.lucaargolo.opticalnetworks.utils.handlers.BlockEntityScreenHandler
import io.github.lucaargolo.opticalnetworks.utils.widgets.GhostSlot
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class AttachmentScreenHandler(syncId: Int, playerInventory: PlayerInventory, entity: AttachmentBlockEntity, context: ScreenHandlerContext): BlockEntityScreenHandler<AttachmentBlockEntity>(syncId, playerInventory, entity, context), GhostSlot.IScreenHandler {

    override val ghostSlots = mutableListOf<GhostSlot>()
    override fun getGhostInv(): DefaultedList<ItemStack> = entity.ghostInv
    override fun getBlockEntity(): BlockEntity = entity

    init {
        (0..2).forEach { n ->
            (0..2).forEach { m ->
                ghostSlots.add(GhostSlot(m + (n * 3), 7 + (m + 3) * 18, n * 18 + 16))
            }
        }

        (0..2).forEach { n ->
            (0..8).forEach { m ->
                addSlot(Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 84 + n * 18))
            }
        }

        (0..8).forEach { n ->
            addSlot(Slot(playerInventory, n,  8 + n * 18, 142))
        }
    }

    override fun transferSlot(player: PlayerEntity, invSlot: Int): ItemStack? {
        return ItemStack.EMPTY
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return context.run({ world: World, blockPos: BlockPos ->
            if (world.getBlockState(
                    blockPos
                ).block != entity.block
            ) false else player.squaredDistanceTo(
                blockPos.x + .5,
                blockPos.y + .5,
                blockPos.z + .5
            ) < 64.0
        }, true)
    }

}