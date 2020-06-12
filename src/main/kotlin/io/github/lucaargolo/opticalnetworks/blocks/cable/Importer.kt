package io.github.lucaargolo.opticalnetworks.blocks.cable

import net.minecraft.block.BlockState
import net.minecraft.state.property.Properties
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes

class Importer: Exporter() {

    override fun getSpacificShape(state: BlockState): VoxelShape {
        val facing = state[Properties.FACING]
        return when(facing) {
            Direction.NORTH -> VoxelShapes.union(
                createCuboidShape(6.0, 6.0, 0.0, 10.0, 10.0, 6.0),
                createCuboidShape(4.0, 4.0, 2.0, 12.0, 12.0, 4.0)
            )
            Direction.SOUTH -> VoxelShapes.union(
                createCuboidShape(6.0, 6.0, 10.0, 10.0, 10.0, 16.0),
                createCuboidShape(4.0, 4.0, 12.0, 12.0, 12.0, 14.0)
            )
            Direction.EAST -> VoxelShapes.union(
                createCuboidShape(10.0, 6.0, 6.0, 16.0, 10.0, 10.0),
                createCuboidShape(12.0, 4.0, 4.0, 14.0, 12.0, 12.0)
            )
            Direction.WEST -> VoxelShapes.union(
                createCuboidShape(0.0, 6.0, 6.0, 6.0, 10.0, 10.0),
                createCuboidShape(2.0, 4.0, 4.0, 4.0, 12.0, 12.0)
            )
            Direction.UP -> VoxelShapes.union(
                createCuboidShape(6.0, 10.0, 6.0, 10.0, 16.0, 10.0),
                createCuboidShape(4.0, 12.0, 4.0, 12.0, 14.0, 12.0)
            )
            Direction.DOWN -> VoxelShapes.union(
                createCuboidShape(6.0, 0.0, 6.0, 10.0, 6.0, 10.0),
                createCuboidShape(4.0, 2.0, 4.0, 12.0, 4.0, 12.0)
            )
            else -> createCuboidShape(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
    }
}