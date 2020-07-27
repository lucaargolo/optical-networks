package io.github.lucaargolo.opticalnetworks.blocks.miscellaneous

import io.github.lucaargolo.opticalnetworks.worldgen.BANANA_TREE_FEATURE_CONFIG
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.Material
import net.minecraft.block.SaplingBlock
import net.minecraft.block.sapling.SaplingGenerator
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldView
import java.util.*

class BananaSapling: SaplingBlock(object: SaplingGenerator() { override fun createTreeFeature(random: Random?, bl: Boolean) = BANANA_TREE_FEATURE_CONFIG }, FabricBlockSettings.of(Material.PLANT).sounds(BlockSoundGroup.GRASS).noCollision().ticksRandomly().breakInstantly()) {
    override fun canPlaceAt(state: BlockState?, world: WorldView, pos: BlockPos): Boolean {
        val blockPos = pos.down()
        return world.getBlockState(blockPos).isOf(Blocks.SAND) || canPlantOnTop(world.getBlockState(blockPos), world, blockPos)
    }
}