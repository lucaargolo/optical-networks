@file:Suppress("unused")

package io.github.lucaargolo.opticalnetworks

import io.github.lucaargolo.opticalnetworks.blocks.*
import io.github.lucaargolo.opticalnetworks.blocks.terminal.TerminalScreen
import io.github.lucaargolo.opticalnetworks.blocks.terminal.TerminalScreenHandler
import io.github.lucaargolo.opticalnetworks.items.initItems
import io.github.lucaargolo.opticalnetworks.items.initItemsClient
import io.github.lucaargolo.opticalnetworks.network.NetworkState
import io.github.lucaargolo.opticalnetworks.network.initNetworkInteractPacket
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.LiteralText
import net.minecraft.util.Identifier

const val mOD_ID = "opticalnetworks"
val uPDATE_CURSOR_SLOT = Identifier(mOD_ID, "update_cursor_slot")


fun init() {
    initBlocks()
    initItems()
    initNetworkInteractPacket()
    ContainerProviderRegistry.INSTANCE.registerFactory(getBlockId(TERMINAL)) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
        val pos = packetByteBuf.readBlockPos()
        val tag = packetByteBuf.readCompoundTag()
        val network = NetworkState.Network(null, playerEntity.world);
        network.fromTag(tag!!)
        TerminalScreenHandler(syncId,
            playerEntity.inventory,
            network,
            ScreenHandlerContext.create(playerEntity.world, pos)
        )
    }

}

fun initClient() {
    initBlocksClient()
    initItemsClient()
    ScreenProviderRegistry.INSTANCE.registerFactory(getBlockId(TERMINAL)) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
        val pos = packetByteBuf.readBlockPos()
        val tag = packetByteBuf.readCompoundTag()
        val network = NetworkState.Network(null, playerEntity.world);
        network.fromTag(tag!!)
        TerminalScreen(
            TerminalScreenHandler(
                syncId,
                playerEntity.inventory,
                network,
                ScreenHandlerContext.EMPTY
            ), playerEntity.inventory, LiteralText("Terminal")
        )
    }
    ClientSidePacketRegistry.INSTANCE.register(uPDATE_CURSOR_SLOT) { packetContext: PacketContext, attachedData: PacketByteBuf ->
        val stack = attachedData.readItemStack()
        packetContext.taskQueue.execute {
            packetContext.player.inventory.cursorStack = stack
        }
    }
    BlockRenderLayerMap.INSTANCE.putBlock(CABLE, RenderLayer.getTranslucent())
}

