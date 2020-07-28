package io.github.lucaargolo.opticalnetworks.network.entity

import io.github.lucaargolo.opticalnetworks.blocks.controller.Controller
import io.github.lucaargolo.opticalnetworks.blocks.getEntityType
import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.network.blocks.CableConnectable
import io.github.lucaargolo.opticalnetworks.utils.getNetworkState
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.util.Tickable
import java.awt.Color

open class NetworkBlockEntity(val block: Block): BlockEntity(getEntityType(block)), Tickable, BlockEntityClientSerializable {

    var currentNetwork: Network? = null;
    open var currentColor: Color? = null
        set(value) {
            if(world?.isClient == true && value != field)
                MinecraftClient.getInstance().worldRenderer.updateBlock(world, pos, cachedState, cachedState, 0)
            field = value
        }

    private fun isNetworkInvalid(): Boolean {
        return currentNetwork == null || !currentNetwork!!.isValid() || currentNetwork!!.storedPower < (this.block as? CableConnectable)?.energyUsage ?: 0.0
    }

    override fun tick() {
        if(world?.isClient == false) {
            val networkState = getNetworkState(world as ServerWorld)
            currentNetwork = networkState.getNetwork(world as ServerWorld, pos)
            if(currentNetwork == null) {
                networkState.updateBlock(world as ServerWorld, pos)
                currentNetwork = networkState.getNetwork(world as ServerWorld, pos)
            }
            if(isNetworkInvalid()) {
                if(cachedState[Properties.ENABLED])
                    world!!.setBlockState(pos, cachedState.with(Properties.ENABLED, false))
            }else{
                if(!cachedState[Properties.ENABLED])
                    world!!.setBlockState(pos, cachedState.with(Properties.ENABLED, true))
                currentNetwork!!.storedPower -= (this.block as? CableConnectable)?.energyUsage ?: 0.0
            }
        }
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        currentColor?.let {tag.putInt("color", it.rgb)}
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        super.fromTag(state, tag)
        currentColor = if(tag.contains("color")) Color(tag.getInt("color")) else null
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag {
        return toTag(tag)
    }

    override fun fromClientTag(tag: CompoundTag) {
        fromTag(block.defaultState, tag)
    }

}