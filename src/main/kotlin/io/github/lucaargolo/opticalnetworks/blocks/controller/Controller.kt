package io.github.lucaargolo.opticalnetworks.blocks.controller

import io.github.lucaargolo.opticalnetworks.blocks.getBlockId
import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectableWithEntity
import io.github.lucaargolo.opticalnetworks.utils.getNetworkState
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.LiteralText
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World

class Controller: NetworkConnectableWithEntity(FabricBlockSettings.of(Material.METAL)) {

    override fun createBlockEntity(world: BlockView?): BlockEntity? {
        return ControllerBlockEntity(this)
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        return if(world.getBlockEntity(pos) is ControllerBlockEntity) {
            if(!world.isClient) {
                player.sendMessage(LiteralText("stored energy: ${(world.getBlockEntity(pos) as ControllerBlockEntity).storedPower}"), false)
                val network = getNetworkState(world as ServerWorld)
                    .getNetwork(world, pos)
                if(network == null) {
                    player.sendMessage(LiteralText("This is not a valid network!"), false)
                }else{
                    val tag = network.toTag(CompoundTag())
                    ContainerProviderRegistry.INSTANCE.openContainer(getBlockId(this), player as ServerPlayerEntity?) { buf ->
                        buf.writeBlockPos(pos)
                        buf.writeCompoundTag(tag)
                    }
                }

            }
            ActionResult.SUCCESS
        }else super.onUse(state, world, pos, player, hand, hit)
    }
}