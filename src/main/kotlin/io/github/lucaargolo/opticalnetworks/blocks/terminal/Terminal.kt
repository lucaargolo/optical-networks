package io.github.lucaargolo.opticalnetworks.blocks.terminal

import io.github.lucaargolo.opticalnetworks.blocks.getBlockId
import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectable
import io.github.lucaargolo.opticalnetworks.network.NetworkState
import io.github.lucaargolo.opticalnetworks.network.getNetworkState
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.text.LiteralText
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class Terminal: NetworkConnectable(FabricBlockSettings.of(Material.METAL)) {

    init {
        defaultState = stateManager.defaultState.with(Properties.HORIZONTAL_FACING, Direction.SOUTH)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(Properties.HORIZONTAL_FACING)
        super.appendProperties(builder)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        return defaultState.with(Properties.HORIZONTAL_FACING, ctx.playerFacing.opposite)
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            val network = getNetworkState(world as ServerWorld).getNetwork(world, pos)
            if(network == null) {
                player.sendMessage(LiteralText("This is not a valid network!"), false)
            }else{
                val tag = network.toTag(CompoundTag())
                ContainerProviderRegistry.INSTANCE.openContainer(getBlockId(this), player as ServerPlayerEntity?) { buf ->
                    buf.writeBlockPos(pos)
                    buf.writeCompoundTag(tag)
                }
            }

        }
        return ActionResult.SUCCESS
    }

}