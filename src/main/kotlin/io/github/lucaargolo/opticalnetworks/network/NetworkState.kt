package io.github.lucaargolo.opticalnetworks.network

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.blocks.cable.Cable
import io.github.lucaargolo.opticalnetworks.blocks.controller.Controller
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerBlockEntity
import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectable
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.World
import java.util.*

class NetworkState: PersistentState(MOD_ID) {

    var networks = mutableListOf<Network>()
    var networksByPos = mutableMapOf<World, MutableMap<BlockPos, Network>>()

    fun getNetworkByUUID(uuid: UUID): Network? {
        networks.forEach {
            if(it.id == uuid) return it
        }
        return null
    }

    fun updateBlock(world: World, pos: BlockPos) {
        val blockState = world.getBlockState(pos)
        val block = blockState.block
        if(block is NetworkConnectable && getNetwork(world, pos) == null) {
            val adjacentNetworks = mutableListOf<Network>()
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
            if(adjacentNetworks.size > 0) {
                adjacentNetworks[0].addComponent(pos, block)
                adjacentNetworks.remove(adjacentNetworks[0]);
                adjacentNetworks.forEach {
                    networks.remove(it)
                }
            }else{
                createNetwork(world, pos)
            }
        }else if(block !is NetworkConnectable && getNetwork(world, pos) != null) {
            val network = getNetwork(world, pos)!!
            network.removeComponent(pos)
        }
        cacheNetworksPos()
    }

    private fun createNetwork(world: World, pos: BlockPos): Network? {
        val n = Network(this, world);
        val posSet = populateNetwork(mutableSetOf(), world, pos);
        posSet.forEach {
            val blockState = world.getBlockState(it)
            val block = blockState.block
            n.components.add(it)
            n.componentsMap[it] = block;
            if(block is Controller) {
                if(n.controller == null) n.controller = it
                else n.controller = null
            }
        }
        if(n.controller != null) {
            networks.add(n)
            n.components.forEach { p ->
                world.players.forEach { pl ->
                    val passedData = PacketByteBuf(Unpooled.buffer())
                    passedData.writeBlockPos(p)
                    passedData.writeInt((world.getBlockEntity(n.controller) as ControllerBlockEntity).storedColor.rgb)
                    ServerSidePacketRegistry.INSTANCE.sendToPlayer(pl, UPDATE_COLOR_MAP_S2C_PACKET, passedData)
                }
            }
            return n;
        }else{
            return null;
        }
    }

    private fun populateNetwork(nc: MutableSet<BlockPos>, world: World, pos: BlockPos): MutableSet<BlockPos> {
        val blockState = world.getBlockState(pos)
        val block = blockState.block
        if(block !is NetworkConnectable) return nc;
        nc.add(pos)
        if(block is Cable) {
            if(blockState[Properties.NORTH] && !nc.contains(pos.north())) nc.addAll(populateNetwork(nc, world, pos.north()))
            if(blockState[Properties.SOUTH] && !nc.contains(pos.south())) nc.addAll(populateNetwork(nc, world, pos.south()))
            if(blockState[Properties.EAST] && !nc.contains(pos.east())) nc.addAll(populateNetwork(nc, world, pos.east()))
            if(blockState[Properties.WEST] && !nc.contains(pos.west())) nc.addAll(populateNetwork(nc, world, pos.west()))
            if(blockState[Properties.UP] && !nc.contains(pos.up())) nc.addAll(populateNetwork(nc, world, pos.up()))
            if(blockState[Properties.DOWN] && !nc.contains(pos.down())) nc.addAll(populateNetwork(nc, world, pos.down()))
        }else{
            if(!nc.contains(pos.north())) nc.addAll(populateNetwork(nc, world, pos.north()))
            if(!nc.contains(pos.south())) nc.addAll(populateNetwork(nc, world, pos.south()))
            if(!nc.contains(pos.east())) nc.addAll(populateNetwork(nc, world, pos.east()))
            if(!nc.contains(pos.west())) nc.addAll(populateNetwork(nc, world, pos.west()))
            if(!nc.contains(pos.up())) nc.addAll(populateNetwork(nc, world, pos.up()))
            if(!nc.contains(pos.down())) nc.addAll(populateNetwork(nc, world, pos.down()))
        }
        return nc
    }

    fun updateNetwork(network: Network) {
        networks.remove(network)
        val updatedPos = mutableSetOf<BlockPos>()
        network.components.forEach {
            if(!updatedPos.contains(it)) {
                val newNetwork = createNetwork(network.world, it);
                newNetwork?.components?.let { componentPos ->
                    updatedPos.addAll(componentPos)
                }
            }
        }
    }

    private fun cacheNetworksPos() {
        networksByPos = mutableMapOf()
        networks.forEach {
            if(networksByPos[it.world] == null) networksByPos[it.world] = mutableMapOf()
            it.components.forEach { pos ->
                networksByPos[it.world]!![pos] = it
            }
        }

    }

    fun getNetwork(world: World, pos: BlockPos): Network? {
        return networksByPos[world]?.get(pos)
    }


    override fun toTag(tag: CompoundTag): CompoundTag {
        return tag;
    }

    override fun fromTag(tag: CompoundTag) {}

}