package io.github.lucaargolo.opticalnetworks.blocks.cable

import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.ConnectingBlock
import net.minecraft.item.ItemPlacementContext
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.World
import net.minecraft.world.WorldAccess

abstract class CableAttachment: Cable(), BlockEntityProvider {

    init {
        defaultState = stateManager.defaultState.with(Properties.FACING, Direction.DOWN)
    }

    override fun onSyncedBlockEvent(state: BlockState?, world: World, pos: BlockPos?, type: Int, data: Int): Boolean {
        super.onSyncedBlockEvent(state, world, pos, type, data)
        val blockEntity = world.getBlockEntity(pos)
        return blockEntity?.onSyncedBlockEvent(type, data) ?: false
    }

    override fun createScreenHandlerFactory(state: BlockState?, world: World, pos: BlockPos?): NamedScreenHandlerFactory? {
        val blockEntity = world.getBlockEntity(pos)
        return if (blockEntity is NamedScreenHandlerFactory) blockEntity else null
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(Properties.FACING)
        super.appendProperties(builder)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
        val facing = ctx.side.opposite
        val state = super.getPlacementState(ctx).with(Properties.FACING, facing)
        return updateFacing(facing, state)
    }

    override fun getStateForNeighborUpdate(state: BlockState, facing: Direction, neighborState: BlockState, world: WorldAccess, pos: BlockPos, neighborPos: BlockPos): BlockState {
        val newState = super.getStateForNeighborUpdate(state, facing, neighborState, world, pos, neighborPos)
        val newFacing = state[Properties.FACING]
        return updateFacing(newFacing, newState)
    }

    open fun getSpacificShape(state: BlockState): VoxelShape {
        val facing = state[Properties.FACING]
        return when(facing) {
            Direction.NORTH -> VoxelShapes.union(
                createCuboidShape(6.0, 6.0, 2.0, 10.0, 10.0, 6.0),
                createCuboidShape(4.0, 4.0, 0.0, 12.0, 12.0, 2.0)
            )
            Direction.SOUTH -> VoxelShapes.union(
                createCuboidShape(6.0, 6.0, 10.0, 10.0, 10.0, 14.0),
                createCuboidShape(4.0, 4.0, 14.0, 12.0, 12.0, 16.0)
            )
            Direction.EAST -> VoxelShapes.union(
                createCuboidShape(10.0, 6.0, 6.0, 14.0, 10.0, 10.0),
                createCuboidShape(14.0, 4.0, 4.0, 16.0, 12.0, 12.0)
            )
            Direction.WEST -> VoxelShapes.union(
                createCuboidShape(2.0, 6.0, 6.0, 6.0, 10.0, 10.0),
                createCuboidShape(0.0, 4.0, 4.0, 2.0, 12.0, 12.0)
            )
            Direction.UP -> VoxelShapes.union(
                createCuboidShape(6.0, 10.0, 6.0, 10.0, 14.0, 10.0),
                createCuboidShape(4.0, 14.0, 4.0, 12.0, 16.0, 12.0)
            )
            Direction.DOWN -> VoxelShapes.union(
                createCuboidShape(6.0, 2.0, 6.0, 10.0, 6.0, 10.0),
                createCuboidShape(4.0, 0.0, 4.0, 12.0, 2.0, 12.0)
            )
            else -> createCuboidShape(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
    }

    override fun getShape(state: BlockState): VoxelShape {
        return VoxelShapes.union(getSpacificShape(state), super.getShape(state))
    }

    private fun updateFacing(facing: Direction, state: BlockState): BlockState {
        return when(facing) {
            Direction.NORTH -> state.with(ConnectingBlock.NORTH, false)
            Direction.SOUTH -> state.with(ConnectingBlock.SOUTH, false)
            Direction.EAST -> state.with(ConnectingBlock.EAST, false)
            Direction.WEST -> state.with(ConnectingBlock.WEST, false)
            Direction.UP -> state.with(ConnectingBlock.UP, false)
            Direction.DOWN -> state.with(ConnectingBlock.DOWN, false)
            else -> state
        }
    }
}