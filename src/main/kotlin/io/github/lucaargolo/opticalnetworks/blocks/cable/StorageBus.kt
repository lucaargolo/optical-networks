package io.github.lucaargolo.opticalnetworks.blocks.cable

import net.minecraft.block.BlockState
import net.minecraft.state.property.Properties
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes

class StorageBus: Exporter() {

    override fun getSpacificShape(state: BlockState): VoxelShape {
        val facing = state[Properties.FACING]
        return VoxelShapes.union(when(facing) {
            Direction.NORTH -> VoxelShapes.union(
                createCuboidShape(6.0, 6.0, 2.0, 10.0, 10.0, 6.0),
                createCuboidShape(1.0, 1.0, 0.0, 15.0, 15.0, 1.0)
            )
            Direction.SOUTH -> VoxelShapes.union(
                createCuboidShape(6.0, 6.0, 10.0, 10.0, 10.0, 14.0),
                createCuboidShape(1.0, 1.0, 15.0, 15.0, 15.0, 16.0)
            )
            Direction.EAST -> VoxelShapes.union(
                createCuboidShape(10.0, 6.0, 6.0, 14.0, 10.0, 10.0),
                createCuboidShape(15.0, 1.0, 1.0, 16.0, 15.0, 15.0)
            )
            Direction.WEST -> VoxelShapes.union(
                createCuboidShape(2.0, 6.0, 6.0, 6.0, 10.0, 10.0),
                createCuboidShape(0.0, 1.0, 1.0, 1.0, 15.0, 15.0)
            )
            Direction.UP -> VoxelShapes.union(
                createCuboidShape(6.0, 10.0, 6.0, 10.0, 14.0, 10.0),
                createCuboidShape(1.0, 14.0, 1.0, 15.0, 15.0, 15.0)
            )
            Direction.DOWN -> VoxelShapes.union(
                createCuboidShape(6.0, 2.0, 6.0, 10.0, 6.0, 10.0),
                createCuboidShape(1.0, 0.0, 1.0, 15.0, 1.0, 15.0)
            )
            else -> createCuboidShape(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }, super.getSpacificShape(state))
    }
}