package io.github.lucaargolo.opticalnetworks.blocks.controller

import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import io.github.lucaargolo.opticalnetworks.utils.getNetworkState
import net.minecraft.block.Block
import net.minecraft.block.BlockState
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

    override var currentColor: Color? = getRandomColor()

    private fun getRandomColor(): Color {
        val rand = world?.random ?: Random()
        return Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
    }

    var storedPower = 0.0

    override fun setStored(p0: Double) {
        if(currentNetwork?.mainController != pos) {
            val be = currentNetwork?.mainController?.let{ pos -> (world!!.getBlockEntity(pos) as? ControllerBlockEntity) }
            be?.let {
                it.setStored(it.storedPower+(p0-storedPower))
            }
        }else{
            storedPower = p0
            markDirty()
            sync()
        }
    }

    override fun getMaxStoredPower() = if(currentNetwork?.mainController == pos) currentNetwork?.getMaxStoredPower() ?: 100000.0 else 100000.0

    override fun getTier() = EnergyTier.INFINITE

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