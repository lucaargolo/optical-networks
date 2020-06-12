@file:Suppress("unused")

package io.github.lucaargolo.opticalnetworks

import io.github.lucaargolo.opticalnetworks.blocks.*
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreen
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.terminal.TerminalScreen
import io.github.lucaargolo.opticalnetworks.blocks.terminal.TerminalScreenHandler
import io.github.lucaargolo.opticalnetworks.items.initItems
import io.github.lucaargolo.opticalnetworks.items.initItemsClient
import io.github.lucaargolo.opticalnetworks.network.NetworkState
import io.github.lucaargolo.opticalnetworks.network.colorMap
import io.github.lucaargolo.opticalnetworks.network.initNetworkColorHandlerClient
import io.github.lucaargolo.opticalnetworks.network.initNetworkInteractPacket
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.resource.ResourceManager
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.LiteralText
import net.minecraft.util.Identifier
import java.awt.Color
import java.util.*
import java.util.function.Consumer

const val MOD_ID = "opticalnetworks"
val UPDATE_CURSOR_SLOT = Identifier(MOD_ID, "update_cursor_slot")

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

    ContainerProviderRegistry.INSTANCE.registerFactory(getBlockId(CONTROLLER)) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
        val pos = packetByteBuf.readBlockPos()
        val tag = packetByteBuf.readCompoundTag()
        val network = NetworkState.Network(null, playerEntity.world);
        network.fromTag(tag!!)
        ControllerScreenHandler(syncId,
            playerEntity.inventory,
            network,
            ScreenHandlerContext.create(playerEntity.world, pos)
        )
    }

}

fun initClient() {
    initBlocksClient()
    initItemsClient()
    initNetworkColorHandlerClient()
    ModelLoadingRegistry.INSTANCE.registerAppender { _: ResourceManager?, out: Consumer<ModelIdentifier?> ->
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_base"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_off"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_1"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_2"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_3"), ""))
    }
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
    ScreenProviderRegistry.INSTANCE.registerFactory(getBlockId(CONTROLLER)) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
        val pos = packetByteBuf.readBlockPos()
        val tag = packetByteBuf.readCompoundTag()
        val network = NetworkState.Network(null, playerEntity.world);
        network.fromTag(tag!!)
        ControllerScreen(
            ControllerScreenHandler(
                syncId,
                playerEntity.inventory,
                network,
                ScreenHandlerContext.EMPTY
            ), playerEntity.inventory, LiteralText("Controller")
        )
    }
    ClientSidePacketRegistry.INSTANCE.register(UPDATE_CURSOR_SLOT) { packetContext: PacketContext, attachedData: PacketByteBuf ->
        val stack = attachedData.readItemStack()
        packetContext.taskQueue.execute {
            packetContext.player.inventory.cursorStack = stack
        }
    }

}

