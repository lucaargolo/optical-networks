package io.github.lucaargolo.opticalnetworks.blocks

import io.github.lucaargolo.opticalnetworks.CLIENT
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
import io.github.lucaargolo.opticalnetworks.blocks.terminal.*
import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import io.github.lucaargolo.opticalnetworks.worldgen.BANANA_TREE_FEATURE_CONFIG
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.sapling.SaplingGenerator
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.client.render.RenderLayer
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldView
import java.util.*

val blockRegistry = mutableMapOf<Identifier, ModBlock>()

val CONTROLLER = if(CLIENT)
        register(Identifier(MOD_ID, "controller"), ModBlockWithEntity<ControllerBlockEntity>(Controller(), null, ControllerScreenHandler::class, ControllerScreen::class))
    else
        register(Identifier(MOD_ID, "controller"), ModBlockWithEntity<ControllerBlockEntity>(Controller(), null, ControllerScreenHandler::class, null))

val TERMINAL = if(CLIENT)
        register(Identifier(MOD_ID, "terminal"), ModBlockWithEntity<BlockEntity>(Terminal(), null, TerminalScreenHandler::class, TerminalScreen::class))
    else
        register(Identifier(MOD_ID, "terminal"), ModBlockWithEntity<BlockEntity>(Terminal(), null, TerminalScreenHandler::class, null))

val CRAFTING_TERMINAL = if(CLIENT)
        register(Identifier(MOD_ID, "crafting_terminal"), ModBlockWithEntity<CraftingTerminalBlockEntity>(Terminal.Crafting(), null, CraftingTerminalScreenHandler::class, CraftingTerminalScreen::class))
    else
        register(Identifier(MOD_ID, "crafting_terminal"), ModBlockWithEntity<CraftingTerminalBlockEntity>(Terminal.Crafting(), null, CraftingTerminalScreenHandler::class, null))

val BLUEPRINT_TERMINAL = register(Identifier(MOD_ID, "blueprint_terminal"), ModBlockWithEntity<BlueprintTerminalBlockEntity>(Terminal.Blueprint(), null, null, null))

val DRIVE_RACK = if(CLIENT)
        register(Identifier(MOD_ID, "drive_rack"), ModBlockWithEntity<DriveRackBlockEntity>(DriveRack(), DriveRackBlockEntityRenderer::class, DriveRackScreenHandler::class, DriveRackScreen::class))
    else
        register(Identifier(MOD_ID, "drive_rack"), ModBlockWithEntity<DriveRackBlockEntity>(DriveRack(), null, DriveRackScreenHandler::class, null))

val CRAFTING_COMPUTER = if(CLIENT)
        register(Identifier(MOD_ID, "crafting_computer"), ModBlockWithEntity<CraftingComputerBlockEntity>(CraftingComputer(), CraftingComputerBlockEntityRenderer::class, CraftingComputerScreenHandler::class, CraftingComputerScreen::class))
    else
        register(Identifier(MOD_ID, "crafting_computer"), ModBlockWithEntity<CraftingComputerBlockEntity>(CraftingComputer(), null, CraftingComputerScreenHandler::class, null))
val INTERFACE = if(CLIENT)
        register(Identifier(MOD_ID, "interface"), ModBlockWithEntity<InterfaceBlockEntity>(Interface(), null, InterfaceScreenHandler::class, InterfaceScreen::class))
    else
        register(Identifier(MOD_ID, "interface"), ModBlockWithEntity<InterfaceBlockEntity>(Interface(), null, InterfaceScreenHandler::class, null))

val ASSEMBLER = if(CLIENT)
        register(Identifier(MOD_ID, "assembler"), ModBlockWithEntity<AssemblerBlockEntity>(Assembler(), null, AssemblerScreenHandler::class, AssemblerScreen::class))
    else
        register(Identifier(MOD_ID, "assembler"), ModBlockWithEntity<AssemblerBlockEntity>(Assembler(), null, AssemblerScreenHandler::class, null))

val CABLE = register(Identifier(MOD_ID, "cable"), ModBlock(Cable()))
val EXPORTER = if(CLIENT)
        register(Identifier(MOD_ID, "exporter"), ModBlockWithEntity<ExporterBlockEntity>(Exporter(), null, AttachmentScreenHandler::class, AttachmentScreen::class))
    else
        register(Identifier(MOD_ID, "exporter"), ModBlockWithEntity<ExporterBlockEntity>(Exporter(), null, AttachmentScreenHandler::class, null))

val IMPORTER = if(CLIENT)
        register(Identifier(MOD_ID, "importer"), ModBlockWithEntity<ImporterBlockEntity>(Importer(), null, AttachmentScreenHandler::class, AttachmentScreen::class))
    else
        register(Identifier(MOD_ID, "importer"), ModBlockWithEntity<ImporterBlockEntity>(Importer(), null, AttachmentScreenHandler::class, null))

val STORAGE_BUS = register(Identifier(MOD_ID, "storage_bus"), ModBlock(StorageBus()))

val BANANA = register(Identifier(MOD_ID, "banana"), ModBlock(Banana(), false))
val BANANA_SAPLING = register(Identifier(MOD_ID, "banana_sapling"), ModBlock(object: SaplingBlock(object: SaplingGenerator() {
    override fun createTreeFeature(random: Random?, bl: Boolean) = BANANA_TREE_FEATURE_CONFIG
}, FabricBlockSettings.of(Material.PLANT).sounds(BlockSoundGroup.GRASS).noCollision().ticksRandomly().breakInstantly()) {
    override fun canPlaceAt(state: BlockState?, world: WorldView, pos: BlockPos): Boolean {
        val blockPos = pos.down()
        return world.getBlockState(blockPos).isOf(Blocks.SAND) || canPlantOnTop(world.getBlockState(blockPos), world, blockPos)
    }
}))

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
        if(it.value.block == BANANA || it.value.block == BANANA_SAPLING)
            BlockRenderLayerMap.INSTANCE.putBlock(it.value.block, RenderLayer.getCutout())
        else if(!FabricLoader.getInstance().isModLoaded("json-model-extensions"))
            BlockRenderLayerMap.INSTANCE.putBlock(it.value.block, RenderLayer.getTranslucent())
        ColorProviderRegistry.BLOCK.register(BlockColorProvider { state, world, pos, tintIndex ->
            val be = world?.getBlockEntity(pos)
            if(be is NetworkBlockEntity && be.currentColor != null)
                be.currentColor!!.rgb
            else 0x666666
        }, it.value.block)
    }
}
