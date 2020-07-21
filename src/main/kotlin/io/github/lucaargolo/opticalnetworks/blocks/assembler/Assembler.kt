package io.github.lucaargolo.opticalnetworks.blocks.assembler

import io.github.lucaargolo.opticalnetworks.blocks.getBlockId
import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectableWithEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World

class Assembler: NetworkConnectableWithEntity(FabricBlockSettings.of(Material.METAL).nonOpaque()) {

    override val bandwidthUsage = 50.0
    override val energyUsage = 32.0

    override fun createBlockEntity(world: BlockView?): BlockEntity? {
        return AssemblerBlockEntity(this)
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            ContainerProviderRegistry.INSTANCE.openContainer(getBlockId(this), player as ServerPlayerEntity?) { buf ->
                buf.writeBlockPos(pos)
            }
        }
        return ActionResult.SUCCESS
    }


    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, notify: Boolean) {
        if (!state.isOf(newState.block)) {
            (world.getBlockEntity(pos) as? AssemblerBlockEntity)?.let {
                ItemScatterer.spawn(world, pos, it)
            }
            super.onStateReplaced(state, world, pos, newState, notify)
        }
    }

}