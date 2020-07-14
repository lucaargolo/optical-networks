package io.github.lucaargolo.opticalnetworks.network.blocks

import io.github.lucaargolo.opticalnetworks.utils.getNetworkState
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

abstract class NetworkConnectable(settings: Settings): Block(settings) {

    override fun onPlaced(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity?, itemStack: ItemStack) {
        if(world is ServerWorld) {
            val networkState = getNetworkState(world)
            networkState.updateBlock(world, pos)
        }
        super.onPlaced(world, pos, state, placer, itemStack)
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, notify: Boolean) {
        if (!state.isOf(newState.block)) {
            if(world is ServerWorld) {
                val networkState = getNetworkState(world)
                networkState.updateBlock(world, pos)
            }
            super.onStateReplaced(state, world, pos, newState, notify)
        }
    }

}