package io.github.lucaargolo.opticalnetworks.blocks.miscellaneous

import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.*
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.tag.BlockTags
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldView
import java.util.*

class Banana: Block(FabricBlockSettings.of(Material.PLANT).ticksRandomly().strength(0.2f, 3.0f).sounds(BlockSoundGroup.WOOD).nonOpaque().noCollision()), Fertilizable {

    init {
        defaultState = defaultState.with(Properties.AGE_2, 0)
    }

    override fun getOutlineShape(state: BlockState?, world: BlockView?, pos: BlockPos?, context: ShapeContext?): VoxelShape {
        return createCuboidShape(12.0, 16.0, 12.0, 4.0, 4.0, 4.0)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(Properties.AGE_2)
        super.appendProperties(builder)
    }

    override fun hasRandomTicks(state: BlockState): Boolean {
        return state.get(Properties.AGE_2) < 2
    }

    override fun randomTick(state: BlockState, world: ServerWorld, pos: BlockPos?, random: Random?) {
        if (world.random.nextInt(5) == 0) {
            val i = state.get(Properties.AGE_2)
            if (i < 2) {
                world.setBlockState(pos, state.with(CocoaBlock.AGE, i + 1), 2)
            }
        }
    }

    override fun canPlaceAt(state: BlockState, world: WorldView, pos: BlockPos): Boolean {
        val block = world.getBlockState(pos.up()).block
        return block == Blocks.JUNGLE_LEAVES
    }

    override fun isFertilizable(world: BlockView?, pos: BlockPos?, state: BlockState, isClient: Boolean): Boolean {
        return state.get(Properties.AGE_2) < 2
    }

    override fun canGrow(world: World?, random: Random?, pos: BlockPos?, state: BlockState?): Boolean {
        return true
    }

    override fun grow(world: ServerWorld, random: Random?, pos: BlockPos?, state: BlockState) {
        world.setBlockState(pos, state.with(Properties.AGE_2, state.get(Properties.AGE_2) + 1), 2)
    }
}