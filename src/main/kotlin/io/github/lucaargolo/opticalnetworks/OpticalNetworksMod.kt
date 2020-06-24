@file:Suppress("unused")

package io.github.lucaargolo.opticalnetworks

import io.github.lucaargolo.opticalnetworks.blocks.*
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreen
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.terminal.TerminalScreen
import io.github.lucaargolo.opticalnetworks.blocks.terminal.TerminalScreenHandler
import io.github.lucaargolo.opticalnetworks.items.initItems
import io.github.lucaargolo.opticalnetworks.items.initItemsClient
import io.github.lucaargolo.opticalnetworks.network.*
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry
import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.resource.ResourceManager
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.LiteralText
import net.minecraft.util.Identifier
import java.util.function.Consumer

const val MOD_ID = "opticalnetworks"

fun init() {
    initBlocks()
    initItems()
    initNetworkPackets()
}

fun initClient() {
    initBlocksClient()
    initItemsClient()
    initNetworkPacketsClient()
    ModelLoadingRegistry.INSTANCE.registerAppender { _: ResourceManager?, out: Consumer<ModelIdentifier?> ->
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_base"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_off"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_1"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_2"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_3"), ""))
    }
}

