package io.github.lucaargolo.opticalnetworks.blocks.crafting

import io.github.lucaargolo.opticalnetworks.blocks.getBlockId
import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectableWithEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView
import net.minecraft.world.World

class CraftingComputer: NetworkConnectableWithEntity(FabricBlockSettings.of(Material.METAL)) {

    init {
        defaultState = defaultState.with(Properties.HORIZONTAL_FACING, Direction.SOUTH)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(Properties.HORIZONTAL_FACING)
        super.appendProperties(builder)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        return defaultState.with(Properties.HORIZONTAL_FACING, ctx.playerFacing.opposite)
    }

    override fun createBlockEntity(world: BlockView?) = CraftingComputerBlockEntity(this)

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            ContainerProviderRegistry.INSTANCE.openContainer(getBlockId(this), player as ServerPlayerEntity?) { buf ->
                buf.writeBlockPos(pos)
            }
        }
        return ActionResult.SUCCESS
    }

}