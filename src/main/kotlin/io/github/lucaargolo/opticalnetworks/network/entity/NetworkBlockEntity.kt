package io.github.lucaargolo.opticalnetworks.network.entity

import io.github.lucaargolo.opticalnetworks.blocks.getEntityType
import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.network.getNetworkState
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.Block
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Tickable

open class NetworkBlockEntity(val block: Block): BlockEntity(getEntityType(block)), Tickable, BlockEntityClientSerializable {

    var currentNetwork: Network? = null;

    override fun tick() {
        if(world?.isClient == false) {
            val networkState = getNetworkState(world as ServerWorld)
            currentNetwork = networkState.getNetwork(world as ServerWorld, pos)
            if(currentNetwork == null) {
                networkState.updateBlock(world as ServerWorld, pos)
                currentNetwork = networkState.getNetwork(world as ServerWorld, pos)
            }
            sync()
        }
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag {
        return toTag(tag)
    }

    override fun fromClientTag(tag: CompoundTag) {
        fromTag(block.defaultState, tag)
    }

}