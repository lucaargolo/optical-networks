package io.github.lucaargolo.opticalnetworks.blocks.controller

import io.github.lucaargolo.opticalnetworks.blocks.getBlockId
import io.github.lucaargolo.opticalnetworks.network.blocks.CableConnectable
import io.github.lucaargolo.opticalnetworks.utils.getNetworkState
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.Properties
import net.minecraft.text.LiteralText
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess

class Controller: BlockWithEntity(FabricBlockSettings.of(Material.METAL)), CableConnectable {

    override fun onPlaced(world: World, pos: BlockPos, state: BlockState, placer: LivingEntity?, itemStack: ItemStack) {
        if(world is ServerWorld) {
            val networkState = getNetworkState(world)
            networkState.updateBlock(world, pos)
        }
        super.onPlaced(world, pos, state, placer, itemStack)
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, notify: Boolean) {
        if (!state.isOf(newState.block)) {
            if(world is ServerWorld) {
                val networkState = getNetworkState(world)
                networkState.updateBlock(world, pos)
            }
            super.onStateReplaced(state, world, pos, newState, notify)
        }
    }

    enum class Pillar: StringIdentifiable {
        NONE,
        AXIS_X,
        AXIS_Y,
        AXIS_Z;
        override fun asString() = name.toLowerCase()
    }

    companion object {
        val PILLAR: EnumProperty<Pillar> = EnumProperty.of("pillar", Pillar::class.java)
    }

    init {
        defaultState = defaultState.with(PILLAR, Pillar.NONE).with(Properties.ENABLED, false)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(PILLAR)
        builder.add(Properties.ENABLED)
        super.appendProperties(builder)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        val north = ctx.world.getBlockState(ctx.blockPos.north()).block == this
        val east = ctx.world.getBlockState(ctx.blockPos.east()).block == this
        val south = ctx.world.getBlockState(ctx.blockPos.south()).block == this
        val west = ctx.world.getBlockState(ctx.blockPos.west()).block == this
        val up = ctx.world.getBlockState(ctx.blockPos.up()).block == this
        val down = ctx.world.getBlockState(ctx.blockPos.down()).block == this
        return if(north && south && !east && !west && !up && !down) defaultState.with(PILLAR, Pillar.AXIS_Z)
        else if(!north && !south && east && west && !up && !down) defaultState.with(PILLAR, Pillar.AXIS_X)
        else if(!north && !south && !east && !west && up && down) defaultState.with(PILLAR, Pillar.AXIS_Y)
        else defaultState.with(PILLAR, Pillar.NONE)
    }

    override fun getStateForNeighborUpdate(state: BlockState, direction: Direction, newState: BlockState, world: WorldAccess, pos: BlockPos, posFrom: BlockPos): BlockState {
        val north = world.getBlockState(pos.north()).block == this
        val east = world.getBlockState(pos.east()).block == this
        val south = world.getBlockState(pos.south()).block == this
        val west = world.getBlockState(pos.west()).block == this
        val up = world.getBlockState(pos.up()).block == this
        val down = world.getBlockState(pos.down()).block == this
        return if(north && south && !east && !west && !up && !down) state.with(PILLAR, Pillar.AXIS_Z)
        else if(!north && !south && east && west && !up && !down) state.with(PILLAR, Pillar.AXIS_X)
        else if(!north && !south && !east && !west && up && down) state.with(PILLAR, Pillar.AXIS_Y)
        else state.with(PILLAR, Pillar.NONE)
    }

    override fun getCollisionShape(state: BlockState, world: BlockView, pos: BlockPos, context: ShapeContext): VoxelShape {
        return getShape(state)
    }

    override fun getOutlineShape(state: BlockState, world: BlockView, pos: BlockPos, context: ShapeContext): VoxelShape {
        return getShape(state)
    }

    open fun getShape(state: BlockState): VoxelShape {
        return when(state[PILLAR]) {
            Pillar.AXIS_X -> createCuboidShape(0.0, 1.0, 1.0, 16.0, 15.0, 15.0)
            Pillar.AXIS_Y -> createCuboidShape(1.0, 0.0, 1.0, 15.0, 16.0, 15.0)
            Pillar.AXIS_Z -> createCuboidShape(1.0, 1.0, 0.0, 15.0, 15.0, 16.0)
            else -> createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
        }
    }

    override fun createBlockEntity(world: BlockView?): BlockEntity? {
        return ControllerBlockEntity(this)
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        return if(world.getBlockEntity(pos) is ControllerBlockEntity) {
            if(!world.isClient) {
                val network = getNetworkState(world as ServerWorld).getNetwork(world, pos)
                if(network == null) {
                    player.sendMessage(LiteralText("This is not a valid network!"), false)
                }else{
                    val tag = network.getOptimizedStateTag(CompoundTag())
                    ContainerProviderRegistry.INSTANCE.openContainer(getBlockId(this), player as ServerPlayerEntity?) { buf ->
                        buf.writeBlockPos(network.mainController)
                        buf.writeCompoundTag(tag)
                        buf.writeUuid(network.id)
                    }
                }

            }
            ActionResult.SUCCESS
        }else super.onUse(state, world, pos, player, hand, hit)
    }

    override fun getRenderType(state: BlockState?): BlockRenderType {
        return BlockRenderType.MODEL
    }
}