package io.github.lucaargolo.opticalnetworks.network.blocks

import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

abstract class NetworkConnectableWithEntity(settings: Settings): NetworkConnectable(settings), BlockEntityProvider {

    init {
        defaultState = stateManager.defaultState.with(Properties.ENABLED, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(Properties.ENABLED)
        super.appendProperties(builder)
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

}