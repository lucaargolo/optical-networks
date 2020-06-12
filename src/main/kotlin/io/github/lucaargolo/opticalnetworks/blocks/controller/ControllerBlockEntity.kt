package io.github.lucaargolo.opticalnetworks.blocks.controller

import io.github.lucaargolo.opticalnetworks.blocks.getEntityType
import io.github.lucaargolo.opticalnetworks.network.NetworkState
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Tickable
import team.reborn.energy.EnergySide
import team.reborn.energy.EnergyStorage
import team.reborn.energy.EnergyTier
import java.awt.Color

class ControllerBlockEntity(val block: Block): BlockEntity(getEntityType(block)), BlockEntityClientSerializable, EnergyStorage, Tickable {

    var currentNetwork: NetworkState.Network? = null;
    var storedPower = 0.0
    var storedColor = Color.ORANGE
        set(value) {
            field = value
            markDirty()
            if(world?.isClient == false) sync()
        }

    override fun tick() {
        if(world?.isClient == false) {
            val networkState = NetworkState.getNetworkState(world as ServerWorld)
            currentNetwork = networkState.getNetwork(world as ServerWorld, pos)
            if(currentNetwork == null) {
                networkState.updateBlock(world as ServerWorld, pos)
            }
            sync()
        }
    }

    override fun setStored(p0: Double) {
        storedPower = p0
        markDirty()
    }

    override fun getMaxStoredPower() = 100000.0

    override fun getTier() = EnergyTier.LOW

    override fun getStored(p0: EnergySide) = storedPower

    override fun toTag(tag: CompoundTag): CompoundTag {
        tag.putInt("storedColor", storedColor.rgb)
        tag.putDouble("storedPower", storedPower)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        storedColor = Color(tag.getInt("storedColor"))
        storedPower = tag.getDouble("storedPower")
        super.fromTag(state, tag)
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag {
        return toTag(tag)
    }

    override fun fromClientTag(tag: CompoundTag) {
        fromTag(block.defaultState, tag)
    }
}