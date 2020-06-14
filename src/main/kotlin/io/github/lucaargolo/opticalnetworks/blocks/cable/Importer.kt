package io.github.lucaargolo.opticalnetworks.blocks.cable

import io.github.lucaargolo.opticalnetworks.blocks.cable.exporter.Exporter
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.state.property.Properties
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView

class Importer: CableAttachment() {

    override fun createBlockEntity(world: BlockView?): BlockEntity? {
        return null
    }
}