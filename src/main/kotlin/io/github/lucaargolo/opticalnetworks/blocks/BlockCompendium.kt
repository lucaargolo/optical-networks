package io.github.lucaargolo.opticalnetworks.blocks

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.Interface
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.InterfaceBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.InterfaceScreen
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.InterfaceScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.assembler.Assembler
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerScreen
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.cable.*
import io.github.lucaargolo.opticalnetworks.blocks.attachment.AttachmentScreen
import io.github.lucaargolo.opticalnetworks.blocks.attachment.AttachmentScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.controller.Controller
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreen
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.crafting.CraftingComputer
import io.github.lucaargolo.opticalnetworks.blocks.crafting.CraftingComputerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.crafting.CraftingComputerScreen
import io.github.lucaargolo.opticalnetworks.blocks.crafting.CraftingComputerScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.*
import io.github.lucaargolo.opticalnetworks.blocks.terminal.*
import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

val blockRegistry = mutableMapOf<Identifier, ModBlock>()

val CABLE = register(Identifier(MOD_ID, "cable"), ModBlock(Cable()))
val EXPORTER = register(Identifier(MOD_ID, "exporter"), ModBlockWithEntity<ExporterBlockEntity>(Exporter(), AttachmentScreenHandler::class, AttachmentScreen::class))
val IMPORTER = register(Identifier(MOD_ID, "importer"), ModBlockWithEntity<ImporterBlockEntity>(Importer(), AttachmentScreenHandler::class, AttachmentScreen::class))
val STORAGE_BUS = register(Identifier(MOD_ID, "storage_bus"), ModBlock(StorageBus()))

val BLUEPRINT_TERMINAL = register(Identifier(MOD_ID, "blueprint_terminal"), ModBlockWithEntity<BlueprintTerminalBlockEntity>(Terminal.Blueprint()))
val CRAFTING_TERMINAL = register(Identifier(MOD_ID, "crafting_terminal"), ModBlockWithEntity<CraftingTerminalBlockEntity>(Terminal.Crafting(), CraftingTerminalScreenHandler::class, CraftingTerminalScreen::class))
val TERMINAL = register(Identifier(MOD_ID, "terminal"), ModBlockWithEntity<BlockEntity>(Terminal(), TerminalScreenHandler::class, TerminalScreen::class))

val INTERFACE = register(Identifier(MOD_ID, "interface"), ModBlockWithEntity<InterfaceBlockEntity>(Interface(), InterfaceScreenHandler::class, InterfaceScreen::class))
val CRAFTING_COMPUTER = register(Identifier(MOD_ID, "crafting_computer"), ModBlockWithEntity<CraftingComputerBlockEntity>(CraftingComputer(), CraftingComputerScreenHandler::class, CraftingComputerScreen::class))
val ASSEMBLER = register(Identifier(MOD_ID, "assembler"), ModBlockWithEntity<AssemblerBlockEntity>(Assembler(), AssemblerScreenHandler::class, AssemblerScreen::class))
val CONTROLLER = register(Identifier(MOD_ID, "controller"), ModBlockWithEntity<ControllerBlockEntity>(Controller(), ControllerScreenHandler::class, ControllerScreen::class))
val DRIVE_RACK = register(Identifier(MOD_ID, "drive_rack"), ModBlockWithEntity<DriveRackBlockEntity>(DriveRack(), DriveRackBlockEntityRenderer::class, DriveRackScreenHandler::class, DriveRackScreen::class))

private fun register(identifier: Identifier, block: ModBlock): Block {
    blockRegistry[identifier] = block
    return block.block
}

fun getBlockId(block: Block): Identifier? {
    blockRegistry.forEach {
        if(it.value.block == block) return it.key
    }
    return null
}

fun getEntityType(block: Block): BlockEntityType<out BlockEntity>? {
    blockRegistry.forEach {
        if(it.value.block == block) return (it.value as ModBlockWithEntity<*>).entity
    }
    return null
}

fun initBlocks() {
    blockRegistry.forEach{ it.value.init(it.key) }
}

fun initBlocksClient() {
    blockRegistry.forEach{
        it.value.initClient(it.key)
        if(!FabricLoader.getInstance().isModLoaded("json-model-extensions"))
            BlockRenderLayerMap.INSTANCE.putBlock(it.value.block, RenderLayer.getTranslucent())
        ColorProviderRegistry.BLOCK.register(BlockColorProvider { state, world, pos, tintIndex ->
            val be = world?.getBlockEntity(pos)
            if(be is NetworkBlockEntity && be.currentColor != null)
                be.currentColor!!.rgb
            else 0x666666
        }, it.value.block)
    }
}
