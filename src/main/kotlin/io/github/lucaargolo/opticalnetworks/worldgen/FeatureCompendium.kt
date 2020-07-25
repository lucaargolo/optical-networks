package io.github.lucaargolo.opticalnetworks.worldgen

import io.github.lucaargolo.opticalnetworks.worldgen.banana.BananaTreeDecorator
import io.github.lucaargolo.opticalnetworks.worldgen.banana.BananaTreeFoliagePlacer
import net.minecraft.block.Blocks
import net.minecraft.world.gen.decorator.TreeDecorator
import net.minecraft.world.gen.feature.ConfiguredFeature
import net.minecraft.world.gen.feature.TreeFeature
import net.minecraft.world.gen.feature.TreeFeatureConfig
import net.minecraft.world.gen.feature.size.TwoLayersFeatureSize
import net.minecraft.world.gen.stateprovider.SimpleBlockStateProvider
import net.minecraft.world.gen.trunk.StraightTrunkPlacer

val BANANA_TREE_FEATURE = TreeFeature(TreeFeatureConfig.CODEC)
val BANANA_TREE_FEATURE_CONFIG: ConfiguredFeature<TreeFeatureConfig, *> = BANANA_TREE_FEATURE.configure(
        TreeFeatureConfig.Builder(
            SimpleBlockStateProvider(Blocks.JUNGLE_LOG.defaultState),
            SimpleBlockStateProvider(Blocks.JUNGLE_LEAVES.defaultState),
            BananaTreeFoliagePlacer(0, 0, 0, 0),
            StraightTrunkPlacer(4, 2, 0),
            TwoLayersFeatureSize(0, 0, 0)
        ).decorators(listOf<TreeDecorator>(BananaTreeDecorator(1F))).build())
