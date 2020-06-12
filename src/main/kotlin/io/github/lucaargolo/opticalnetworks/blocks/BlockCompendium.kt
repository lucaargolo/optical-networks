package io.github.lucaargolo.opticalnetworks.blocks

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.blocks.assembler.Assembler
import io.github.lucaargolo.opticalnetworks.blocks.cable.Cable
import io.github.lucaargolo.opticalnetworks.blocks.cable.Exporter
import io.github.lucaargolo.opticalnetworks.blocks.cable.Importer
import io.github.lucaargolo.opticalnetworks.blocks.cable.StorageBus
import io.github.lucaargolo.opticalnetworks.blocks.controller.Controller
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.*
import io.github.lucaargolo.opticalnetworks.blocks.terminal.Terminal
import io.github.lucaargolo.opticalnetworks.network.colorMap
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.util.Identifier

val blockRegistry = mutableMapOf<Identifier, ModBlock>()

val CABLE = register(Identifier(MOD_ID, "cable"), ModBlock(Cable()))
val EXPORTER = register(Identifier(MOD_ID, "exporter"), ModBlock(Exporter()))
val IMPORTER = register(Identifier(MOD_ID, "importer"), ModBlock(Importer()))
val STORAGE_BUS = register(Identifier(MOD_ID, "storage_bus"), ModBlock(StorageBus()))

val TERMINAL = register(Identifier(MOD_ID, "terminal"), ModBlock(Terminal()))

val ASSEMBLER = register(Identifier(MOD_ID, "assembler"), ModBlock(Assembler()))
val CONTROLLER = register(Identifier(MOD_ID, "controller"), ModBlockWithEntity<ControllerBlockEntity>(Controller()))
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
        ColorProviderRegistry.BLOCK.register(BlockColorProvider { state, world, pos, tintIndex ->
            colorMap.get(pos)?.rgb ?: 0
        }, it.value.block)
    }
}
