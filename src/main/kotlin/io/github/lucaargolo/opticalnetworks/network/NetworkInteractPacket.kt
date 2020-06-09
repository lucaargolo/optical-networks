package io.github.lucaargolo.opticalnetworks.network

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.UPDATE_CURSOR_SLOT
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.PacketContext
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier

val NETWORK_INTERACT = Identifier(MOD_ID, "network_interact")

fun initNetworkInteractPacket() {

    ServerSidePacketRegistry.INSTANCE.register(NETWORK_INTERACT) { packetContext: PacketContext, attachedData: PacketByteBuf ->

        val player = packetContext.player
        val playerInventory = player.inventory
        val networkId = attachedData.readUuid()
        val networkState = NetworkState.getNetworkState(player.world as ServerWorld)
        val network = networkState.getNetworkByUUID(networkId)
        if(network != null) {

            val type = attachedData.readInt()

            when(type) {
                1 -> {
                    //mouseClicked in TerminalScreen
                    val button = attachedData.readInt()
                    val shift = attachedData.readBoolean()
                    val stack = attachedData.readItemStack()
                    packetContext.taskQueue.execute {
                        executeMouseClicker(stack, playerInventory, shift, network, button)
                    }
                }
            }
        }
    }

}

private fun updateCursorStack(playerInventory: PlayerInventory, stack: ItemStack) {
    val passedData = PacketByteBuf(Unpooled.buffer())
    passedData.writeItemStack(stack)
    playerInventory.cursorStack = stack
    ServerSidePacketRegistry.INSTANCE.sendToPlayer(playerInventory.player, UPDATE_CURSOR_SLOT, passedData)
}

private fun executeMouseClicker(stack: ItemStack, playerInventory: PlayerInventory, shift: Boolean, network: NetworkState.Network, button: Int) {
    if (shift) {
        val copyStack = stack.copy()
        playerInventory.insertStack(stack)
        if(!stack.isEmpty) copyStack.decrement(stack.count)
        network.removeStack(copyStack)
        if(!copyStack.isEmpty) {
            playerInventory.main.remove(copyStack)
        }
    }else{
        if (button == 0) {
            if (playerInventory.cursorStack.isEmpty) {
                val copyStack = stack.copy()
                network.removeStack(stack)
                if(stack.isEmpty) updateCursorStack(playerInventory, copyStack)
                else {
                    copyStack.decrement(stack.count)
                    updateCursorStack(playerInventory, copyStack)
                }
            } else {
                val insertStack = network.insertStack(playerInventory.cursorStack)
                updateCursorStack(playerInventory, insertStack)
            }
        }
        if (button == 1) {
            if (playerInventory.cursorStack.isEmpty) {
                stack.decrement(stack.count / 2)
                val copyStack = stack.copy()
                network.removeStack(stack)
                if(stack.isEmpty) updateCursorStack(playerInventory, copyStack)
                else {
                    copyStack.decrement(stack.count)
                    updateCursorStack(playerInventory, copyStack)
                }
            } else {
                val oneStack = playerInventory.cursorStack.copy()
                oneStack.count = 1;
                val resultStack = network.insertStack(oneStack)
                if (resultStack.isEmpty) {
                    playerInventory.cursorStack.decrement(1)
                    val newStack = playerInventory.cursorStack
                    updateCursorStack(playerInventory, newStack)
                }
            }
        }
    }
}