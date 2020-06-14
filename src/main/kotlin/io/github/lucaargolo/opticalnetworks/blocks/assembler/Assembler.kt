package io.github.lucaargolo.opticalnetworks.blocks.assembler

import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectableWithEntity
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.world.BlockView

class Assembler: NetworkConnectableWithEntity(FabricBlockSettings.of(Material.METAL)) {

    override fun createBlockEntity(world: BlockView?): BlockEntity? {
        return null
    }

}