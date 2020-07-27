package io.github.lucaargolo.opticalnetworks.blocks.cable

import io.github.lucaargolo.opticalnetworks.network.blocks.CableConnectable
import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectable
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.item.ItemPlacementContext
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.DyeColor
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.WorldAccess

open class Cable(val dyeColor: DyeColor? = null): NetworkConnectable(FabricBlockSettings.of(Material.GLASS).strength(0.3F).requiresTool().sounds(BlockSoundGroup.GLASS).nonOpaque()) {

    override val bandwidthUsage = 0.0
    override val energyUsage = 0.0

    init {
        defaultState = stateManager.defaultState
            .with(Properties.NORTH, false)
            .with(Properties.SOUTH, false)
            .with(Properties.EAST, false)
            .with(Properties.WEST, false)
            .with(Properties.UP, false)
            .with(Properties.DOWN, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(Properties.NORTH)
        builder.add(Properties.SOUTH)
        builder.add(Properties.EAST)
        builder.add(Properties.WEST)
        builder.add(Properties.UP)
        builder.add(Properties.DOWN)
        super.appendProperties(builder)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
        return defaultState
            .with(ConnectingBlock.NORTH, ctx.world.getBlockState(ctx.blockPos.north()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
            .with(ConnectingBlock.SOUTH, ctx.world.getBlockState(ctx.blockPos.south()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
            .with(ConnectingBlock.EAST, ctx.world.getBlockState(ctx.blockPos.east()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
            .with(ConnectingBlock.WEST, ctx.world.getBlockState(ctx.blockPos.west()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
            .with(ConnectingBlock.UP, ctx.world.getBlockState(ctx.blockPos.up()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
            .with(ConnectingBlock.DOWN, ctx.world.getBlockState(ctx.blockPos.down()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
    }

    override fun getStateForNeighborUpdate(state: BlockState, facing: Direction, neighborState: BlockState, world: WorldAccess, pos: BlockPos, neighborPos: BlockPos): BlockState {
        return state
            .with(ConnectingBlock.NORTH, world.getBlockState(pos.north()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
            .with(ConnectingBlock.SOUTH, world.getBlockState(pos.south()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
            .with(ConnectingBlock.EAST, world.getBlockState(pos.east()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
            .with(ConnectingBlock.WEST, world.getBlockState(pos.west()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
            .with(ConnectingBlock.UP, world.getBlockState(pos.up()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
            .with(ConnectingBlock.DOWN, world.getBlockState(pos.down()).block.let { if(this.dyeColor != null && it is Cable && it.dyeColor != null) it == this else it is CableConnectable})
    }

    override fun getCollisionShape(state: BlockState, world: BlockView, pos: BlockPos, context: ShapeContext): VoxelShape {
        return getShape(state)
    }

    override fun getOutlineShape(state: BlockState, world: BlockView, pos: BlockPos, context: ShapeContext): VoxelShape {
        return getShape(state)
    }

    open fun getShape(state: BlockState): VoxelShape {
        val shapeList = mutableListOf<VoxelShape>()
        if(state[Properties.NORTH]) shapeList.add(createCuboidShape(6.0, 6.0, 0.0, 10.0, 10.0, 6.0))
        if(state[Properties.SOUTH]) shapeList.add(createCuboidShape(6.0, 6.0, 10.0, 10.0, 10.0, 16.0))
        if(state[Properties.EAST]) shapeList.add(createCuboidShape(10.0, 6.0, 6.0, 16.0, 10.0, 10.0))
        if(state[Properties.WEST]) shapeList.add(createCuboidShape(0.0, 6.0, 6.0, 6.0, 10.0, 10.0))
        if(state[Properties.UP]) shapeList.add(createCuboidShape(6.0, 10.0, 6.0, 10.0, 16.0, 10.0))
        if(state[Properties.DOWN]) shapeList.add(createCuboidShape(6.0, 0.0, 6.0, 10.0, 6.0, 10.0))
        var shape = createCuboidShape(6.0, 6.0, 6.0, 10.0, 10.0, 10.0)
        while(shapeList.isNotEmpty()) {
            shape = VoxelShapes.union(shape, shapeList[0])
            shapeList.removeAt(0)
        }
        return shape
    }

}