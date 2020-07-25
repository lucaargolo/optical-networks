package io.github.lucaargolo.opticalnetworks.blocks.terminal

import io.github.lucaargolo.opticalnetworks.blocks.getBlockId
import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectableWithEntity
import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import io.github.lucaargolo.opticalnetworks.utils.getNetworkState
import io.github.lucaargolo.opticalnetworks.utils.widgets.TerminalSlot
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.block.MaterialColor
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemPlacementContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.text.LiteralText
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView
import net.minecraft.world.World

open class Terminal: NetworkConnectableWithEntity(FabricBlockSettings.of(Material.METAL, MaterialColor.IRON).requiresTool().strength(5.0F, 6.0F).sounds(BlockSoundGroup.METAL)) {

    override val bandwidthUsage = 10.0
    override val energyUsage = 16.0

    interface IScreenHandler {
        val network: Network
        val terminalSlots: MutableList<TerminalSlot>
    }

    init {
        defaultState = defaultState.with(Properties.HORIZONTAL_FACING, Direction.SOUTH)
    }

    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(Properties.HORIZONTAL_FACING)
        super.appendProperties(builder)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? {
        return defaultState.with(Properties.HORIZONTAL_FACING, ctx.playerFacing.opposite)
    }

    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if (!world.isClient) {
            val network = getNetworkState(world as ServerWorld).getNetwork(world, pos)
            if(network == null) {
                player.sendMessage(LiteralText("This is not a valid network!"), false)
            }else{
                val tag = network.getOptimizedStateTag(CompoundTag())
                ContainerProviderRegistry.INSTANCE.openContainer(getBlockId(this), player as ServerPlayerEntity?) { buf ->
                    buf.writeBlockPos(pos)
                    buf.writeCompoundTag(tag)
                    buf.writeUuid(network.id)
                }
            }

        }
        return ActionResult.SUCCESS
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, notify: Boolean) {
        if (!state.isOf(newState.block)) {
            (world.getBlockEntity(pos) as? Inventory)?.let {
                ItemScatterer.spawn(world, pos, it)
            }
            super.onStateReplaced(state, world, pos, newState, notify)
        }
    }

    override fun createBlockEntity(world: BlockView?): BlockEntity? {
        return NetworkBlockEntity(this)
    }

    class Crafting: Terminal() {
        override fun createBlockEntity(world: BlockView?) = CraftingTerminalBlockEntity(this)
    }

    class Blueprint: Terminal() {
        override fun createBlockEntity(world: BlockView?) = BlueprintTerminalBlockEntity(this)

        override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
            if (!world.isClient) {
                val network = getNetworkState(world as ServerWorld).getNetwork(world, pos)
                val entity = world.getBlockEntity(pos)
                if(network == null || entity !is BlueprintTerminalBlockEntity) {
                    player.sendMessage(LiteralText("This is not a valid network!"), false)
                }else{
                    val identifier = getBlockId(this)!!
                    val mode = entity.currentMode
                    if(mode == 0) {
                        val newIdentifier = Identifier(identifier.namespace, identifier.path+"_crafting")
                        val tag = network.getOptimizedStateTag(CompoundTag())
                        ContainerProviderRegistry.INSTANCE.openContainer(newIdentifier, player as ServerPlayerEntity?) { buf ->
                            buf.writeBlockPos(pos)
                            buf.writeCompoundTag(tag)
                            buf.writeUuid(network.id)
                        }
                    }else {
                        val newIdentifier = Identifier(identifier.namespace, identifier.path+"_processing")
                        val tag = network.getOptimizedStateTag(CompoundTag())
                        ContainerProviderRegistry.INSTANCE.openContainer(newIdentifier, player as ServerPlayerEntity?) { buf ->
                            buf.writeBlockPos(pos)
                            buf.writeCompoundTag(tag)
                            buf.writeUuid(network.id)
                        }
                    }

                }

            }
            return ActionResult.SUCCESS
        }
    }

}