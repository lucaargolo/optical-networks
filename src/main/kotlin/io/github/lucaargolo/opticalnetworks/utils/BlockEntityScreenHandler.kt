package io.github.lucaargolo.opticalnetworks.utils

import io.github.lucaargolo.opticalnetworks.blocks.cable.exporter.ExporterBlockEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext

abstract class BlockEntityScreenHandler<T: BlockEntity>(syncId: Int, playerInventory: PlayerInventory, val entity: T, val context: ScreenHandlerContext): ScreenHandler(null, syncId)
