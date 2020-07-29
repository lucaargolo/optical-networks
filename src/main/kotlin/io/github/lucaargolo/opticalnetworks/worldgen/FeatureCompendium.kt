package io.github.lucaargolo.opticalnetworks.worldgen

import com.google.common.collect.ImmutableList
import io.github.lucaargolo.opticalnetworks.worldgen.banana.BananaTreeDecorator
import io.github.lucaargolo.opticalnetworks.worldgen.banana.BananaTreeFoliagePlacer
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import net.minecraft.world.ModifiableTestableWorld
import net.minecraft.world.gen.decorator.TreeDecorator
import net.minecraft.world.gen.feature.ConfiguredFeature
import net.minecraft.world.gen.feature.TreeFeature
import net.minecraft.world.gen.feature.TreeFeatureConfig
import net.minecraft.world.gen.feature.size.TwoLayersFeatureSize
import net.minecraft.world.gen.foliage.FoliagePlacer
import net.minecraft.world.gen.stateprovider.SimpleBlockStateProvider
import net.minecraft.world.gen.trunk.StraightTrunkPlacer
import net.minecraft.world.gen.trunk.TrunkPlacer
import java.util.*

val BANANA_TREE_FEATURE = TreeFeature(TreeFeatureConfig.CODEC)
val BANANA_TREE_FEATURE_CONFIG: ConfiguredFeature<TreeFeatureConfig, *> = BANANA_TREE_FEATURE.configure(
        TreeFeatureConfig.Builder(
            SimpleBlockStateProvider(Blocks.JUNGLE_LOG.defaultState),
            SimpleBlockStateProvider(Blocks.JUNGLE_LEAVES.defaultState),
            BananaTreeFoliagePlacer(0, 0, 0, 0),
            object: StraightTrunkPlacer(4, 2, 0) {
                override fun generate(world: ModifiableTestableWorld?, random: Random?, trunkHeight: Int, pos: BlockPos?, set: MutableSet<BlockPos>?, blockBox: BlockBox?, treeFeatureConfig: TreeFeatureConfig?): MutableList<FoliagePlacer.TreeNode> {
                    for (i in 0 until trunkHeight) {
                        TrunkPlacer.method_27402(world, random, pos!!.up(i), set, blockBox, treeFeatureConfig)
                    }
                    return ImmutableList.of(FoliagePlacer.TreeNode(pos!!.up(trunkHeight), 0, false))
                }
            },
            TwoLayersFeatureSize(0, 0, 0)
        ).decorators(listOf<TreeDecorator>(BananaTreeDecorator(1F))).build())
