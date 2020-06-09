package io.github.lucaargolo.opticalnetworks.blocks.controller

import io.github.lucaargolo.opticalnetworks.blocks.getEntityType
import io.github.lucaargolo.opticalnetworks.network.NetworkState
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Tickable
import team.reborn.energy.EnergySide
import team.reborn.energy.EnergyStorage
import team.reborn.energy.EnergyTier

class ControllerBlockEntity(val block: Block): BlockEntity(getEntityType(block)), BlockEntityClientSerializable, EnergyStorage, Tickable {

    var currentNetwork: NetworkState.Network? = null;
    var storedEnergy = 0.0

    override fun tick() {
        if(world?.isClient == false) {
            val networkState = NetworkState.getNetworkState(world as ServerWorld)
            currentNetwork = networkState.getNetwork(world as ServerWorld, pos)
            if(currentNetwork == null) {
                println("no network oopsie, lets create one ")
                networkState.updateBlock(world as ServerWorld, pos)
            }
        }
    }

    override fun setStored(p0: Double) {
        storedEnergy = p0
        markDirty()
    }

    override fun getMaxStoredPower() = 100000.0

    override fun getTier() = EnergyTier.LOW

    override fun getStored(p0: EnergySide) = storedEnergy

    override fun toTag(tag: CompoundTag?): CompoundTag {
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState?, tag: CompoundTag?) {
        super.fromTag(state, tag)
    }

    override fun toClientTag(p0: CompoundTag?): CompoundTag {
        return toTag(p0)
    }

    override fun fromClientTag(p0: CompoundTag?) {
        fromTag(block.defaultState, p0)
    }
}