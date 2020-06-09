package io.github.lucaargolo.opticalnetworks.blocks.controller

import io.github.lucaargolo.opticalnetworks.network.NetworkConnectable
import io.github.lucaargolo.opticalnetworks.network.NetworkConnectableWithEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
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
                player.sendMessage(LiteralText("stored energy: ${(world.getBlockEntity(pos) as ControllerBlockEntity).storedEnergy}"), false)
            }
            ActionResult.SUCCESS
        }else super.onUse(state, world, pos, player, hand, hit)
    }
}