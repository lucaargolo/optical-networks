package io.github.lucaargolo.opticalnetworks.blocks.cable

import io.github.lucaargolo.opticalnetworks.blocks.getBlockId
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World

open class Exporter: CableWithEntity() {

    override val bandwidthUsage = 50.0
    override val energyUsage = 32.0

    override fun createBlockEntity(world: BlockView?): BlockEntity? {
        return ExporterBlockEntity(this)
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            ContainerProviderRegistry.INSTANCE.openContainer(getBlockId(this), player as ServerPlayerEntity?) { buf ->
                buf.writeBlockPos(pos)
            }
        }
        return ActionResult.SUCCESS
    }

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