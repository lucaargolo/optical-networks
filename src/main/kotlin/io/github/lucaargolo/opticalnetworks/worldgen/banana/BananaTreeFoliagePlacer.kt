package io.github.lucaargolo.opticalnetworks.worldgen.banana

import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import net.minecraft.world.ModifiableTestableWorld
import net.minecraft.world.gen.feature.TreeFeatureConfig
import net.minecraft.world.gen.foliage.FoliagePlacer
import net.minecraft.world.gen.foliage.FoliagePlacerType
import java.util.*

class BananaTreeFoliagePlacer(radius: Int, randomRadius: Int, offset: Int, randomOffset: Int): FoliagePlacer(radius, randomRadius, offset, randomOffset) {

    override fun generate(world: ModifiableTestableWorld, random: Random, config: TreeFeatureConfig, trunkHeight: Int, treeNode: TreeNode, foliageHeight: Int, radius: Int, leaves: MutableSet<BlockPos>, i: Int, blockBox: BlockBox?) {
        val pos = treeNode.center.down(2)
        leaves.add(BlockPos(pos.x, pos.y + 2, pos.z))
        leaves.add(BlockPos(pos.x, pos.y + 1, pos.z + 1))
        leaves.add(BlockPos(pos.x, pos.y + 1, pos.z + 2))
        leaves.add(BlockPos(pos.x, pos.y + 1, pos.z + 3))
        leaves.add(BlockPos(pos.x, pos.y, pos.z + 4))
        leaves.add(BlockPos((pos.x + 1), pos.y + 1, pos.z))
        leaves.add(BlockPos((pos.x + 2), pos.y + 1, pos.z))
        leaves.add(BlockPos((pos.x + 3), pos.y + 1, pos.z))
        leaves.add(BlockPos((pos.x + 4), pos.y, pos.z))
        leaves.add(BlockPos(pos.x, pos.y + 1, pos.z - 1))
        leaves.add(BlockPos(pos.x, pos.y + 1, pos.z - 2))
        leaves.add(BlockPos(pos.x, pos.y + 1, pos.z - 3))
        leaves.add(BlockPos(pos.x, pos.y, pos.z - 4))
        leaves.add(BlockPos((pos.x - 1), pos.y + 1, pos.z))
        leaves.add(BlockPos((pos.x - 1), pos.y + 1, pos.z - 1))
        leaves.add(BlockPos((pos.x - 1), pos.y + 1, pos.z + 1))
        leaves.add(BlockPos((pos.x + 1), pos.y + 1, pos.z - 1))
        leaves.add(BlockPos((pos.x + 1), pos.y + 1, pos.z + 1))
        leaves.add(BlockPos((pos.x - 2), pos.y + 1, pos.z))
        leaves.add(BlockPos((pos.x - 3), pos.y + 1, pos.z))
        leaves.add(BlockPos((pos.x - 4), pos.y, pos.z))
        leaves.add(BlockPos((pos.x + 2), pos.y + 1, pos.z + 2))
        leaves.add(BlockPos((pos.x + 2), pos.y + 1, pos.z - 2))
        leaves.add(BlockPos((pos.x - 2), pos.y + 1, pos.z + 2))
        leaves.add(BlockPos((pos.x - 2), pos.y + 1, pos.z - 2))
        leaves.add(BlockPos((pos.x + 3), pos.y, pos.z + 3))
        leaves.add(BlockPos((pos.x + 3), pos.y, pos.z - 3))
        leaves.add(BlockPos((pos.x - 3), pos.y, pos.z + 3))
        leaves.add(BlockPos((pos.x - 3), pos.y, pos.z - 3))
        leaves.forEach {
            val state = config.leavesProvider.getBlockState(random, pos).with(Properties.PERSISTENT, true)
            world.setBlockState(it, state, 19)
        }
    }

    override fun getHeight(random: Random?, trunkHeight: Int, config: TreeFeatureConfig?): Int {
        return 0
    }

    override fun getType(): FoliagePlacerType<*> {
        return FoliagePlacerType.BLOB_FOLIAGE_PLACER
    }

    override fun isInvalidForLeaves(random: Random?, baseHeight: Int, dx: Int, dy: Int, dz: Int, bl: Boolean): Boolean {
        return false
    }
}