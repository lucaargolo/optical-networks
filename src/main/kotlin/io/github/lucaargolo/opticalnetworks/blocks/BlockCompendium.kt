@file:Suppress("UNCHECKED_CAST", "unused")

package io.github.lucaargolo.opticalnetworks.blocks

import io.github.lucaargolo.opticalnetworks.CLIENT
import io.github.lucaargolo.opticalnetworks.CREATIVE_TAB
import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.Interface
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.InterfaceBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.InterfaceScreen
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.InterfaceScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.assembler.Assembler
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerScreen
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.attachment.AttachmentScreen
import io.github.lucaargolo.opticalnetworks.blocks.attachment.AttachmentScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.cable.*
import io.github.lucaargolo.opticalnetworks.blocks.controller.Controller
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreen
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.crafting.*
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.*
import io.github.lucaargolo.opticalnetworks.blocks.miscellaneous.Banana
import io.github.lucaargolo.opticalnetworks.blocks.miscellaneous.BananaSapling
import io.github.lucaargolo.opticalnetworks.blocks.terminal.*
import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.network.NetworkState
import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import io.github.lucaargolo.opticalnetworks.utils.ModBlockItem
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.state.property.Properties
import net.minecraft.text.TranslatableText
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import java.util.function.Supplier
import kotlin.reflect.KClass

class ContainerInfo(handler: KClass<*>, screen: Supplier<KClass<*>>, val identifier: Identifier? = null) {
    val handler = handler as KClass<ScreenHandler>
    val screen = screen as Supplier<KClass<HandledScreen<*>>>
}

class BlockInfo<T: BlockEntity> (
    val identifier: Identifier,
    private val block: Block,
    private val hasModBlock: Boolean,
    var entity: BlockEntityType<T>?,
    var renderer: KClass<BlockEntityRenderer<T>>?,
    var containers: List<ContainerInfo>
){

    fun init() {
        Registry.register(Registry.BLOCK, identifier, block)
        if(hasModBlock)
            Registry.register(Registry.ITEM, identifier, ModBlockItem(block, Item.Settings().group(CREATIVE_TAB)))
        if (entity != null) {
            Registry.register(Registry.BLOCK_ENTITY_TYPE, identifier, entity)
        }
        containers.forEach{ info ->
            ContainerProviderRegistry.INSTANCE.registerFactory(info.identifier ?: identifier) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
                val pos = packetByteBuf.readBlockPos()
                if(info.handler.java.constructors[0].parameterCount == 5) {
                    val tag = packetByteBuf.readCompoundTag()
                    val state = NetworkState.fromTag(playerEntity.world, tag!!)
                    val uuid = packetByteBuf.readUuid()
                    val network = state.getNetworkByUUID(uuid)
                    info.handler.java.constructors[0].newInstance(syncId,
                        playerEntity.inventory,
                        network,
                        playerEntity.world.getBlockEntity(pos),
                        ScreenHandlerContext.create(playerEntity.world, pos)
                    ) as ScreenHandler
                }else{
                    if(info.handler.java.constructors[0].parameterTypes[2] == Network::class.java) {
                        val tag = packetByteBuf.readCompoundTag()
                        val state = NetworkState.fromTag(playerEntity.world, tag!!)
                        val uuid = packetByteBuf.readUuid()
                        val network = state.getNetworkByUUID(uuid)
                        info.handler.java.constructors[0].newInstance(syncId,
                            playerEntity.inventory,
                            network,
                            ScreenHandlerContext.create(playerEntity.world, pos)
                        ) as ScreenHandler
                    }else{
                        info.handler.java.constructors[0].newInstance(syncId,
                            playerEntity.inventory,
                            playerEntity.world.getBlockEntity(pos),
                            ScreenHandlerContext.create(playerEntity.world, pos)
                        ) as ScreenHandler
                    }
                }

            }
        }
    }

    fun initClient() {
        containers.forEach { info ->
            ScreenProviderRegistry.INSTANCE.registerFactory(info.identifier ?: identifier) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
                val pos = packetByteBuf.readBlockPos()
                if(info.handler.java.constructors[0].parameterCount == 5) {
                    val tag = packetByteBuf.readCompoundTag()
                    val state = NetworkState.fromTag(playerEntity.world, tag!!)
                    val uuid = packetByteBuf.readUuid()
                    val network = state.getNetworkByUUID(uuid)
                    info.screen.get().java.constructors[0].newInstance(
                        info.handler.java.constructors[0].newInstance(
                            syncId,
                            playerEntity.inventory,
                            network,
                            playerEntity.entityWorld.getBlockEntity(pos),
                            ScreenHandlerContext.EMPTY
                        ) as ScreenHandler, playerEntity.inventory, TranslatableText("screen.$MOD_ID.${getBlockId(block)?.path}")
                    ) as HandledScreen<*>
                }else{
                    if(info.handler.java.constructors[0].parameterTypes[2] == Network::class.java) {
                        val tag = packetByteBuf.readCompoundTag()
                        val state = NetworkState.fromTag(playerEntity.world, tag!!)
                        val uuid = packetByteBuf.readUuid()
                        val network = state.getNetworkByUUID(uuid)
                        info.screen.get().java.constructors[0].newInstance(
                            info.handler.java.constructors[0].newInstance(
                                syncId,
                                playerEntity.inventory,
                                network,
                                ScreenHandlerContext.EMPTY
                            ) as ScreenHandler, playerEntity.inventory, TranslatableText("screen.$MOD_ID.${getBlockId(block)?.path}")
                        ) as HandledScreen<*>
                    }else{
                        info.screen.get().java.constructors[0].newInstance(
                            info.handler.java.constructors[0].newInstance(
                                syncId,
                                playerEntity.inventory,
                                playerEntity.entityWorld.getBlockEntity(pos),
                                ScreenHandlerContext.EMPTY
                            ) as ScreenHandler, playerEntity.inventory, TranslatableText("screen.$MOD_ID.${getBlockId(block)?.path}")
                        ) as HandledScreen<*>
                    }
                }
            }
        }
        if(renderer != null) {
            BlockEntityRendererRegistry.INSTANCE.register(entity) { it2 ->
                renderer!!.java.constructors[0].newInstance(it2) as BlockEntityRenderer<T>
            }
        }
    }
}

val blockRegistry = linkedMapOf<Block, BlockInfo<*>>()

fun getBlockId(block: Block) = blockRegistry[block]?.identifier
fun getEntityType(block: Block) = blockRegistry[block]?.entity

fun register(identifier: Identifier, block: Block, hasModBlock: Boolean = true): Block {
    val info = BlockInfo<BlockEntity>(identifier, block, hasModBlock, null, null, listOf())
    blockRegistry[block] = info
    return block
}

fun <T: BlockEntity> registerWithEntity(identifier: Identifier, block: Block, hasModBlock: Boolean = true, renderer: Supplier<KClass<*>>? = null, containers: List<ContainerInfo> = listOf()): Block {
    val ent = (block as? BlockEntityProvider)?.let { BlockEntityType.Builder.create(Supplier { it.createBlockEntity(null) }, block).build(null) as BlockEntityType<T> }
    val rnd = if(CLIENT) renderer?.let { it.get() as KClass<BlockEntityRenderer<T>> } else null
    val info = BlockInfo(identifier, block, hasModBlock, ent, rnd, containers)
    blockRegistry[block] = info
    return block
}

val CONTROLLER = registerWithEntity<ControllerBlockEntity>(Identifier(MOD_ID, "controller"), Controller(), containers = listOf(ContainerInfo(ControllerScreenHandler::class, Supplier { ControllerScreen::class })))
val TERMINAL = registerWithEntity<NetworkBlockEntity>(Identifier(MOD_ID, "terminal"), Terminal(), containers = listOf(ContainerInfo(TerminalScreenHandler::class, Supplier { TerminalScreen::class })))
val CRAFTING_TERMINAL = registerWithEntity<CraftingTerminalBlockEntity>(Identifier(MOD_ID, "crafting_terminal"), Terminal.Crafting(), containers = listOf(ContainerInfo(CraftingTerminalScreenHandler::class, Supplier { CraftingTerminalScreen::class })))
val BLUEPRINT_TERMINAL = registerWithEntity<BlueprintTerminalBlockEntity>(Identifier(MOD_ID, "blueprint_terminal"), Terminal.Blueprint(), containers = listOf(ContainerInfo(BlueprintTerminalScreenHandler.Processing::class, Supplier { BlueprintTerminalScreen.Processing::class }, Identifier(MOD_ID, "blueprint_terminal_processing")), ContainerInfo(BlueprintTerminalScreenHandler.Crafting::class, Supplier { BlueprintTerminalScreen.Crafting::class }, Identifier(MOD_ID, "blueprint_terminal_crafting"))))
val DRIVE_RACK = registerWithEntity<DriveRackBlockEntity>(Identifier(MOD_ID, "drive_rack"), DriveRack(), renderer = Supplier { DriveRackBlockEntityRenderer::class }, containers = listOf(ContainerInfo(DriveRackScreenHandler::class , Supplier { DriveRackScreen::class })))
val CRAFTING_COMPUTER = registerWithEntity<CraftingComputerBlockEntity>(Identifier(MOD_ID, "crafting_computer"), CraftingComputer(), renderer = Supplier {CraftingComputerBlockEntityRenderer::class}, containers = listOf(ContainerInfo(CraftingComputerScreenHandler::class , Supplier { CraftingComputerScreen::class })))
val INTERFACE = registerWithEntity<InterfaceBlockEntity>(Identifier(MOD_ID, "interface"), Interface(), containers = listOf(ContainerInfo(InterfaceScreenHandler::class, Supplier { InterfaceScreen::class })))
val ASSEMBLER = registerWithEntity<AssemblerBlockEntity>(Identifier(MOD_ID, "assembler"), Assembler(), containers = listOf(ContainerInfo(AssemblerScreenHandler::class, Supplier { AssemblerScreen::class })))
val EXPORTER = registerWithEntity<ExporterBlockEntity>(Identifier(MOD_ID, "exporter"), Exporter(), containers = listOf(ContainerInfo(AttachmentScreenHandler::class, Supplier { AttachmentScreen::class })))
val IMPORTER = registerWithEntity<ImporterBlockEntity>(Identifier(MOD_ID, "importer"), Importer(), containers = listOf(ContainerInfo(AttachmentScreenHandler::class, Supplier { AttachmentScreen::class })))
val STORAGE_BUS = register(Identifier(MOD_ID, "storage_bus"), StorageBus())
val CABLE = register(Identifier(MOD_ID, "cable"), Cable())
val WHITE_CABLE = register(Identifier(MOD_ID, "white_cable"), Cable(DyeColor.WHITE))
val ORANGE_CABLE = register(Identifier(MOD_ID, "orange_cable"), Cable(DyeColor.ORANGE))
val MAGENTA_CABLE = register(Identifier(MOD_ID, "magenta_cable"), Cable(DyeColor.MAGENTA))
val LIGHT_BLUE_CABLE = register(Identifier(MOD_ID, "light_blue_cable"), Cable(DyeColor.LIGHT_BLUE))
val YELLOW_CABLE = register(Identifier(MOD_ID, "yellow_cable"), Cable(DyeColor.YELLOW))
val LIME_CABLE = register(Identifier(MOD_ID, "lime_cable"), Cable(DyeColor.LIME))
val PINK_CABLE = register(Identifier(MOD_ID, "pink_cable"), Cable(DyeColor.PINK))
val GRAY_CABLE = register(Identifier(MOD_ID, "gray_cable"), Cable(DyeColor.GRAY))
val LIGHT_GRAY_CABLE = register(Identifier(MOD_ID, "light_gray_cable"), Cable(DyeColor.LIGHT_GRAY))
val CYAN_CABLE = register(Identifier(MOD_ID, "cyan_cable"), Cable(DyeColor.CYAN))
val BLUE_CABLE = register(Identifier(MOD_ID, "blue_cable"), Cable(DyeColor.BLUE))
val PURPLE_CABLE = register(Identifier(MOD_ID, "purple_cable"), Cable(DyeColor.PURPLE))
val GREEN_CABLE = register(Identifier(MOD_ID, "green_cable"), Cable(DyeColor.GREEN))
val BROWN_CABLE = register(Identifier(MOD_ID, "brown_cable"), Cable(DyeColor.BROWN))
val RED_CABLE = register(Identifier(MOD_ID, "red_cable"), Cable(DyeColor.RED))
val BLACK_CABLE = register(Identifier(MOD_ID, "black_cable"), Cable(DyeColor.BLACK))
val BANANA = register(Identifier(MOD_ID, "banana"), Banana(), hasModBlock = false)
val BANANA_SAPLING = register(Identifier(MOD_ID, "banana_sapling"), BananaSapling())


fun initBlocks() {
    blockRegistry.forEach{ it.value.init() }
}

fun initBlocksClient() {
    blockRegistry.forEach{
        it.value.initClient()
        if(it.key == BANANA || it.key == BANANA_SAPLING)
            BlockRenderLayerMap.INSTANCE.putBlock(it.key, RenderLayer.getCutout())
        else if(!FabricLoader.getInstance().isModLoaded("json-model-extensions"))
            BlockRenderLayerMap.INSTANCE.putBlock(it.key, RenderLayer.getTranslucent())
        ColorProviderRegistry.BLOCK.register(BlockColorProvider { state, world, pos, _ ->
            val be = world?.getBlockEntity(pos)
            if(be is NetworkBlockEntity && be.currentColor != null && state[Properties.ENABLED])
                be.currentColor!!.rgb
            else 0x666666
        }, it.key)
    }
}

