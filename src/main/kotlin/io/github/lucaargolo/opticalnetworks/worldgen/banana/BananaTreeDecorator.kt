package io.github.lucaargolo.opticalnetworks.worldgen.banana

import io.github.lucaargolo.opticalnetworks.blocks.BANANA
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldAccess
import net.minecraft.world.gen.decorator.CocoaBeansTreeDecorator
import net.minecraft.world.gen.feature.Feature
import java.util.*

class BananaTreeDecorator(private val f: Float): CocoaBeansTreeDecorator(f) {

    override fun generate(world: WorldAccess?, random: Random, logPositions: List<BlockPos>, leavesPositions: List<BlockPos>, set: Set<BlockPos>, box: BlockBox?) {
        if (random.nextFloat() < f) {
            leavesPositions.forEach {
                if (random.nextFloat() <= 0.25f) {
                    val blockPos2 = it.down()
                    if (Feature.isAir(world, blockPos2)) {
                        val blockState = (BANANA.defaultState.with(Properties.AGE_2, random.nextInt(3)))
                        setBlockStateAndEncompassPosition(world, blockPos2, blockState, set, box)
                    }
                }
            }
        }
    }

}