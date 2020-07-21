package io.github.lucaargolo.opticalnetworks.network

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.blocks.cable.Cable
import io.github.lucaargolo.opticalnetworks.blocks.controller.Controller
import io.github.lucaargolo.opticalnetworks.network.blocks.CableConnectable
import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectable
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.World
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet

class NetworkState(val world: World): PersistentState(MOD_ID) {

    var networks = linkedMapOf<UUID, Network>()
    var networksByPos = linkedMapOf<World, LinkedHashMap<BlockPos, Network>>()

    fun getNetworkByUUID(uuid: UUID) = networks[uuid]!!

    fun updateBlock(world: ServerWorld, pos: BlockPos) {
        val blockState = world.getBlockState(pos)
        val block = blockState.block
        if(block is CableConnectable && getNetwork(world, pos) == null) {
            val adjacentNetworks = linkedSetOf<Network>()
            if(block is Cable) {
                if(blockState[Properties.NORTH]) getNetwork(world, pos.north())?.let { adjacentNetworks.add(it) }
                if(blockState[Properties.SOUTH]) getNetwork(world, pos.south())?.let { adjacentNetworks.add(it) }
                if(blockState[Properties.EAST]) getNetwork(world, pos.east())?.let { adjacentNetworks.add(it) }
                if(blockState[Properties.WEST]) getNetwork(world, pos.west())?.let { adjacentNetworks.add(it) }
                if(blockState[Properties.UP]) getNetwork(world, pos.up())?.let { adjacentNetworks.add(it) }
                if(blockState[Properties.DOWN]) getNetwork(world, pos.down())?.let { adjacentNetworks.add(it) }
            }else{
                getNetwork(world, pos.north())?.let { adjacentNetworks.add(it) }
                getNetwork(world, pos.south())?.let { adjacentNetworks.add(it) }
                getNetwork(world, pos.east())?.let { adjacentNetworks.add(it) }
                getNetwork(world, pos.west())?.let { adjacentNetworks.add(it) }
                getNetwork(world, pos.up())?.let { adjacentNetworks.add(it) }
                getNetwork(world, pos.down())?.let { adjacentNetworks.add(it) }
            }
            var added: Network? = null
            val adjacentNetworksIterator = adjacentNetworks.iterator()
            while(adjacentNetworksIterator.hasNext()){
                val it = adjacentNetworksIterator.next()
                if(added == null) {
                    if (it.type == Network.Type.CONTROLLER && block is Controller) {
                        it.addComponent(pos, block)
                        added = it
                    }else if (it.type == Network.Type.COMPONENTS && block is NetworkConnectable) {
                        it.addComponent(pos, block)
                        added = it
                    }
                }else {
                    if (it.type == added.type) {
                        adjacentNetworksIterator.remove()
                        removeNetwork(it)
                    }
                }
            }
            if(added == null) added = createNetwork(world, pos)
        }else if(block !is CableConnectable && getNetwork(world, pos) != null) {
            val network = getNetwork(world, pos)!!
            network.removeComponent(pos)
        }
        cacheNetworksPos()
    }

    private fun createNetwork(world: ServerWorld, pos: BlockPos): Network {
        val block = world.getBlockState(pos).block
        val type = if(block is Controller) Network.Type.CONTROLLER else Network.Type.COMPONENTS
        val n = Network.create(this, world, type)
        if(type == Network.Type.CONTROLLER) n.mainController = pos
        val posMap = floodFillNetwork(linkedMapOf(), world, pos, type)
        posMap.forEach { (blockPos, netType) ->
            if(netType == type) {
                val blockState = world.getBlockState(blockPos)
                val newBlock = blockState.block
                n.components.add(blockPos)
                n.componentsMap[blockPos] = newBlock
            }else{
                if(netType == Network.Type.COMPONENTS) getNetwork(world, blockPos)?.let {
                    n.componentNetworks.add(it.id)
                    it.controllerNetworks.add(n.id)
                }
                if(netType == Network.Type.CONTROLLER) getNetwork(world, blockPos)?.let {
                    n.controllerNetworks.add(it.id)
                    it.componentNetworks.add(n.id)
                }
            }
        }
        networks[n.id] = n
        n.updateColor()
        return n
    }

    private fun floodFillNetwork(nc: LinkedHashMap<BlockPos, Network.Type>, world: World, pos: BlockPos, type: Network.Type): LinkedHashMap<BlockPos, Network.Type> {
        val blockState = world.getBlockState(pos)
        val block = blockState.block
        if(block !is CableConnectable) return nc
        val blockType = if(block is Controller) Network.Type.CONTROLLER else Network.Type.COMPONENTS
        nc[pos] = blockType
        if(blockType != type) return nc
        if(block is Cable) {
            if(blockState[Properties.NORTH] && !nc.containsKey(pos.north())) nc.putAll(floodFillNetwork(nc, world, pos.north(), type))
            if(blockState[Properties.SOUTH] && !nc.containsKey(pos.south())) nc.putAll(floodFillNetwork(nc, world, pos.south(), type))
            if(blockState[Properties.EAST] && !nc.containsKey(pos.east())) nc.putAll(floodFillNetwork(nc, world, pos.east(), type))
            if(blockState[Properties.WEST] && !nc.containsKey(pos.west())) nc.putAll(floodFillNetwork(nc, world, pos.west(), type))
            if(blockState[Properties.UP] && !nc.containsKey(pos.up())) nc.putAll(floodFillNetwork(nc, world, pos.up(), type))
            if(blockState[Properties.DOWN] && !nc.containsKey(pos.down())) nc.putAll(floodFillNetwork(nc, world, pos.down(), type))
        }else{
            if(!nc.containsKey(pos.north())) nc.putAll(floodFillNetwork(nc, world, pos.north(), type))
            if(!nc.containsKey(pos.south())) nc.putAll(floodFillNetwork(nc, world, pos.south(), type))
            if(!nc.containsKey(pos.east())) nc.putAll(floodFillNetwork(nc, world, pos.east(), type))
            if(!nc.containsKey(pos.west())) nc.putAll(floodFillNetwork(nc, world, pos.west(), type))
            if(!nc.containsKey(pos.up())) nc.putAll(floodFillNetwork(nc, world, pos.up(), type))
            if(!nc.containsKey(pos.down())) nc.putAll(floodFillNetwork(nc, world, pos.down(), type))
        }
        return nc
    }

    private fun removeNetwork(network: Network) {
        network.componentNetworks.forEach {
            networks[it]?.controllerNetworks?.remove(network.id)
        }
        network.controllerNetworks.forEach {
            networks[it]?.componentNetworks?.remove(network.id)
        }
        networks.remove(network.id)
    }

    fun recreateNetwork(network: Network) {
        removeNetwork(network)
        val updatedPos = mutableSetOf<BlockPos>()
        network.components.forEach {
            if(!updatedPos.contains(it)) {
                val newNetwork = createNetwork(network.world as ServerWorld, it);
                newNetwork.components.let { componentPos ->
                    updatedPos.addAll(componentPos)
                }
            }
        }
    }

    private fun cacheNetworksPos() {
        networksByPos = linkedMapOf()
        networks.forEach { (_, network) ->
            if(networksByPos[network.world] == null) networksByPos[network.world] = linkedMapOf()
            network.components.forEach { pos ->
                networksByPos[network.world]!![pos] = network
            }
        }

    }

    fun getNetwork(world: World, pos: BlockPos): Network? {
        return networksByPos[world]?.get(pos)
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        networks.forEach { (uuid, network) ->
            tag.put(uuid.toString(), network.toTag(CompoundTag()))
        }
        return tag
    }

    override fun fromTag(tag: CompoundTag) {
        tag.keys.forEach {
            networks[UUID.fromString(it)] = Network.fromTag(this, world, tag.getCompound(it))
        }
    }

    companion object {
        fun fromTag(world: World, tag: CompoundTag): NetworkState {
            val state = NetworkState(world)
            state.fromTag(tag)
            return state
        }
    }

}