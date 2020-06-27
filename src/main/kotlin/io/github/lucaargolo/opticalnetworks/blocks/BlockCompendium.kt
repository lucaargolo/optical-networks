package io.github.lucaargolo.opticalnetworks.blocks

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.blocks.assembler.Assembler
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerScreen
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.cable.*
import io.github.lucaargolo.opticalnetworks.blocks.cable.attachment.AttachmentScreen
import io.github.lucaargolo.opticalnetworks.blocks.cable.attachment.AttachmentScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.controller.Controller
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreen
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.*
import io.github.lucaargolo.opticalnetworks.blocks.terminal.*
import io.github.lucaargolo.opticalnetworks.network.NETWORK_INTERACT_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.network.REQUEST_COLOR_MAP_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.network.colorMap
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.color.block.BlockColorProvider
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import net.minecraft.world.World

val blockRegistry = mutableMapOf<Identifier, ModBlock>()

val CABLE = register(Identifier(MOD_ID, "cable"), ModBlock(Cable()))
val EXPORTER = register(Identifier(MOD_ID, "exporter"), ModBlockWithEntity<ExporterBlockEntity>(Exporter(), AttachmentScreenHandler::class, AttachmentScreen::class))
val IMPORTER = register(Identifier(MOD_ID, "importer"), ModBlockWithEntity<ImporterBlockEntity>(Importer(), AttachmentScreenHandler::class, AttachmentScreen::class))
val STORAGE_BUS = register(Identifier(MOD_ID, "storage_bus"), ModBlock(StorageBus()))

val BLUEPRINT_TERMINAL = register(Identifier(MOD_ID, "blueprint_terminal"), ModBlockWithEntity<BlueprintTerminalBlockEntity>(Terminal.Blueprint()))
val CRAFTING_TERMINAL = register(Identifier(MOD_ID, "crafting_terminal"), ModBlockWithEntity<CraftingTerminalBlockEntity>(Terminal.Crafting(), CraftingTerminalScreenHandler::class, CraftingTerminalScreen::class))
val TERMINAL = register(Identifier(MOD_ID, "terminal"), ModBlockWithEntity<BlockEntity>(Terminal(), TerminalScreenHandler::class, TerminalScreen::class))

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
        ColorProviderRegistry.BLOCK.register(BlockColorProvider { state, world, pos, tintIndex ->
            val color: Int? = colorMap[pos]?.rgb
                if(color == null) {
                    val passedData = PacketByteBuf(Unpooled.buffer())
                    passedData.writeBlockPos(pos)
                    ClientSidePacketRegistry.INSTANCE.sendToServer(REQUEST_COLOR_MAP_C2S_PACKET, passedData)
                    0x666666
                }else color
        }, it.value.block)
    }
}
