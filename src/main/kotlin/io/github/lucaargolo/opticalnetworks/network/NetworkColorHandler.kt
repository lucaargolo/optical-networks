package io.github.lucaargolo.opticalnetworks.network

import io.github.lucaargolo.opticalnetworks.MOD_ID
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketContext
import net.minecraft.client.MinecraftClient
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.awt.Color

val UPDATE_COLOR = Identifier(MOD_ID, "update_color")

val colorMap = mutableMapOf<BlockPos, Color>()

fun initNetworkColorHandlerClient() {
    ClientSidePacketRegistry.INSTANCE.register(UPDATE_COLOR) { packetContext: PacketContext, attachedData: PacketByteBuf ->
        val pos = attachedData.readBlockPos()
        val color = Color(attachedData.readInt())
        packetContext.taskQueue.execute {
            colorMap[pos] = color
            val world = packetContext.player.world
            val state = world.getBlockState(pos)
            MinecraftClient.getInstance().worldRenderer.updateBlock(world, pos, state, state, 0)
        }
    }
}