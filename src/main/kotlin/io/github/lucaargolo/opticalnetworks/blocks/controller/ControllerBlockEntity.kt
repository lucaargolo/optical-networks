package io.github.lucaargolo.opticalnetworks.blocks.controller

import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import io.github.lucaargolo.opticalnetworks.utils.getNetworkState
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import team.reborn.energy.EnergySide
import team.reborn.energy.EnergyStorage
import team.reborn.energy.EnergyTier
import java.awt.Color
import java.util.*
import kotlin.random.asKotlinRandom

class ControllerBlockEntity(block: Block): NetworkBlockEntity(block), EnergyStorage {

    var networkStoredPowerCache = 0.0

    override var currentColor: Color? = getRandomColor()
        set(value) {
            if(world?.isClient == true && value != field)
                MinecraftClient.getInstance().worldRenderer.updateBlock(world, pos, cachedState, cachedState, 0)
            field = value
        }

    private fun getRandomColor(): Color {
        val rand = world?.random ?: Random()
        return Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
    }

    override fun setStored(p0: Double) {
        currentNetwork?.storedPower = p0
    }

    override fun getTier() = EnergyTier.INFINITE

    override fun getMaxStoredPower() = currentNetwork?.getMaxStoredPower() ?: 0.0

    override fun getStored(p0: EnergySide) = currentNetwork?.storedPower ?: 0.0

    override fun toTag(tag: CompoundTag): CompoundTag {
        tag.putDouble("cache", networkStoredPowerCache)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        networkStoredPowerCache = tag.getDouble("cache")
        super.fromTag(state, tag)
    }

}