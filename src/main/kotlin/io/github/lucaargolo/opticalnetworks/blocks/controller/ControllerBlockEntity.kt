package io.github.lucaargolo.opticalnetworks.blocks.controller

import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.nbt.CompoundTag
import team.reborn.energy.EnergySide
import team.reborn.energy.EnergyStorage
import team.reborn.energy.EnergyTier
import java.awt.Color
import java.util.*
import kotlin.random.asKotlinRandom

class ControllerBlockEntity(block: Block): NetworkBlockEntity(block), EnergyStorage {

    override var currentColor: Color? = getRandomColor()

    private fun getRandomColor(): Color {
        val rand = world?.random ?: Random()
        return Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
    }

    var storedPower = 0.0

    override fun setStored(p0: Double) {
        storedPower = p0
        markDirty()
    }

    override fun getMaxStoredPower() = 100000.0

    override fun getTier() = EnergyTier.LOW

    override fun getStored(p0: EnergySide) = storedPower

    override fun toTag(tag: CompoundTag): CompoundTag {
        tag.putDouble("storedPower", storedPower)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        storedPower = tag.getDouble("storedPower")
        super.fromTag(state, tag)
    }


}