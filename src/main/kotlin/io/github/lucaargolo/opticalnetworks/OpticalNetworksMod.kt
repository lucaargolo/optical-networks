@file:Suppress("unused")

package io.github.lucaargolo.opticalnetworks

import com.mojang.datafixers.util.Pair
import io.github.lucaargolo.opticalnetworks.blocks.*
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreen
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.terminal.*
import io.github.lucaargolo.opticalnetworks.items.blueprint.BlueprintBakedModel
import io.github.lucaargolo.opticalnetworks.items.initItems
import io.github.lucaargolo.opticalnetworks.items.initItemsClient
import io.github.lucaargolo.opticalnetworks.network.*
import io.github.lucaargolo.opticalnetworks.packets.initNetworkPackets
import io.github.lucaargolo.opticalnetworks.packets.initNetworkPacketsClient
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry
import net.fabricmc.fabric.api.client.model.ModelVariantProvider
import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketContext
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.ModelLoader
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.resource.ResourceManager
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Function

const val MOD_ID = "opticalnetworks"

fun init() {
    initBlocks()
    initItems()
    initNetworkPackets()
    ContainerProviderRegistry.INSTANCE.registerFactory(Identifier(MOD_ID, "blueprint_terminal_processing")) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
        val pos = packetByteBuf.readBlockPos()
        val tag = packetByteBuf.readCompoundTag()
        val network = Network(null, playerEntity.world);
        network.fromTag(tag!!)
        BlueprintTerminalScreenHandler.Processing(
            syncId,
            playerEntity.inventory,
            network,
            playerEntity.world.getBlockEntity(pos) as BlueprintTerminalBlockEntity,
            ScreenHandlerContext.create(playerEntity.world, pos)
        )
    }

    ContainerProviderRegistry.INSTANCE.registerFactory(Identifier(MOD_ID, "blueprint_terminal_crafting")) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
        val pos = packetByteBuf.readBlockPos()
        val tag = packetByteBuf.readCompoundTag()
        val network = Network(null, playerEntity.world);
        network.fromTag(tag!!)
        BlueprintTerminalScreenHandler.Crafting(
            syncId,
            playerEntity.inventory,
            network,
            playerEntity.world.getBlockEntity(pos) as BlueprintTerminalBlockEntity,
            ScreenHandlerContext.create(playerEntity.world, pos)
        )
    }
}

fun initClient() {
    initBlocksClient()
    initItemsClient()
    initNetworkPacketsClient()
    ModelLoadingRegistry.INSTANCE.registerAppender { _: ResourceManager?, out: Consumer<ModelIdentifier?> ->
        out.accept(ModelIdentifier(Identifier(MOD_ID, "blueprint_regular"), "inventory"))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_base"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_off"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_1"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_2"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_3"), ""))
    }

    ModelLoadingRegistry.INSTANCE.registerVariantProvider {
        ModelVariantProvider { modelIdentifier, _ ->
            if(modelIdentifier.namespace == MOD_ID && modelIdentifier.path == "blueprint") {
                return@ModelVariantProvider object : UnbakedModel {
                    override fun getModelDependencies(): MutableCollection<Identifier> = mutableListOf()
                    override fun bake(loader: ModelLoader, textureGetter: Function<SpriteIdentifier, Sprite>, rotationScreenHandler: ModelBakeSettings, modelId: Identifier) = BlueprintBakedModel()
                    override fun getTextureDependencies(unbakedModelGetter: Function<Identifier, UnbakedModel>, unresolvedTextureReferences: MutableSet<Pair<String, String>>): MutableCollection<SpriteIdentifier> = mutableListOf()
                }
            }
            return@ModelVariantProvider null
        }
    }

    ScreenProviderRegistry.INSTANCE.registerFactory(Identifier(MOD_ID, "blueprint_terminal_processing")) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
        val pos = packetByteBuf.readBlockPos()
        val tag = packetByteBuf.readCompoundTag()
        val network = Network(null, playerEntity.world);
        network.fromTag(tag!!)
        BlueprintTerminalScreen.Processing(
            BlueprintTerminalScreenHandler.Processing(
                syncId,
                playerEntity.inventory,
                network,
                playerEntity.world.getBlockEntity(pos) as BlueprintTerminalBlockEntity,
                ScreenHandlerContext.create(playerEntity.world, pos)
            ),
            playerEntity.inventory,
            TranslatableText("screen.${MOD_ID}.${getBlockId(BLUEPRINT_TERMINAL)?.path}")
        )
    }

    ScreenProviderRegistry.INSTANCE.registerFactory(Identifier(MOD_ID, "blueprint_terminal_crafting")) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
        val pos = packetByteBuf.readBlockPos()
        val tag = packetByteBuf.readCompoundTag()
        val network = Network(null, playerEntity.world);
        network.fromTag(tag!!)
        BlueprintTerminalScreen.Crafting(
            BlueprintTerminalScreenHandler.Crafting(
                syncId,
                playerEntity.inventory,
                network,
                playerEntity.world.getBlockEntity(pos) as BlueprintTerminalBlockEntity,
                ScreenHandlerContext.create(playerEntity.world, pos)
            ),
            playerEntity.inventory,
            TranslatableText("screen.${MOD_ID}.${getBlockId(BLUEPRINT_TERMINAL)?.path}")
        )
    }
}

