package io.github.lucaargolo.opticalnetworks.blocks.terminal

import io.github.lucaargolo.opticalnetworks.blocks.TERMINAL
import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.network.handlers.NetworkScreenHandler
import io.github.lucaargolo.opticalnetworks.utils.widgets.TerminalSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

open class TerminalScreenHandler(syncId: Int, playerInventory: PlayerInventory, network: Network, context: ScreenHandlerContext): NetworkScreenHandler(syncId, playerInventory, network, context), Terminal.IScreenHandler {

    override val terminalSlots = mutableListOf<TerminalSlot>()

    init {
        (0..6).forEach { n ->
            (0..8).forEach { m ->
                terminalSlots.add(TerminalSlot(8 + m * 18, n * 18 + 18))
            }
        }

        (0..2).forEach { n ->
            (0..8).forEach { m ->
                addSlot(Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 0))
            }
        }

        (0..8).forEach { n ->
            addSlot(Slot(playerInventory, n,  8 + n * 18, 0))
        }
    }

    override fun transferSlot(player: PlayerEntity, invSlot: Int): ItemStack? {
        val stack = slots[invSlot].stack
        return if(!stack.isEmpty) {
            val backupStack = stack.copy()
            val space = network.getSpace()
            val availableSpace = space.second-space.first
            if(stack.count > availableSpace) {
                stack.decrement(availableSpace)
                backupStack.count = availableSpace
                if(!player.world.isClient) network.insertStack(backupStack)
            }else{
                stack.decrement(stack.count)
                if(!player.world.isClient) network.insertStack(backupStack)
            }
            backupStack
        }else ItemStack.EMPTY
    }


    override fun canUse(player: PlayerEntity): Boolean {
        return context.run({ world: World, blockPos: BlockPos ->
            if (world.getBlockState(
                    blockPos
                ).block != TERMINAL
            ) false else player.squaredDistanceTo(
                blockPos.x + .5,
                blockPos.y + .5,
                blockPos.z + .5
            ) < 64.0
        }, true)
    }

}