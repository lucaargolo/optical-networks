package io.github.lucaargolo.opticalnetworks.blocks

import io.github.lucaargolo.opticalnetworks.mOD_ID
import io.github.lucaargolo.opticalnetworks.blocks.basic.Cable
import io.github.lucaargolo.opticalnetworks.blocks.controller.Controller
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.DriveRack
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.DriveRackBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.DriveRackScreen
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.DriveRackScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.terminal.Terminal
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.util.Identifier

val blockRegistry = mutableMapOf<Identifier, ModBlock>()

val CABLE = register(Identifier(mOD_ID, "cable"), ModBlock(Cable()))

val CONTROLLER = register(Identifier(mOD_ID, "controller"), ModBlockWithEntity<ControllerBlockEntity>(Controller()))
val DRIVE_RACK = register(Identifier(mOD_ID, "drive_rack"), ModBlockWithEntity<DriveRackBlockEntity>(DriveRack(), DriveRackScreenHandler::class, DriveRackScreen::class))

val TERMINAL = register(Identifier(mOD_ID, "terminal"), ModBlock(Terminal()))

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
    blockRegistry.forEach{ it.value.initClient(it.key) }
}
