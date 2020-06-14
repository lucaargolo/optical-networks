package io.github.lucaargolo.opticalnetworks.blocks

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectable
import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectableWithEntity
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry
import net.fabricmc.fabric.api.container.ContainerProviderRegistry
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.render.block.entity.BlockEntityRenderer
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.Nameable
import net.minecraft.util.registry.Registry
import java.util.function.Supplier
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST", "unused")
class ModBlockWithEntity<T: BlockEntity>: ModBlock {

    var entity: BlockEntityType<T>? = null
        private set
    private var renderer: KClass<BlockEntityRenderer<T>>? = null
    private var container: KClass<ScreenHandler>? = null
    private var containerScreen: KClass<HandledScreen<*>>? = null

    constructor(block: Block) : super(block) {
        this.entity = if(block is BlockEntityProvider){
            BlockEntityType.Builder.create(Supplier { block.createBlockEntity(null) }, block).build(null) as BlockEntityType<T>
        }else null
    }

    constructor(block: Block, blockEntityRenderer: KClass<*>) : super(block) {
        this.entity = if(block is BlockEntityProvider){
            BlockEntityType.Builder.create(Supplier { block.createBlockEntity(null) }, block).build(null) as BlockEntityType<T>
        }else null
        this.renderer = blockEntityRenderer as KClass<BlockEntityRenderer<T>>
    }


    constructor(block: Block, blockEntityRenderer: KClass<*>, blockEntityScreenHandler: KClass<*>, blockEntityScreen: KClass<*>) : super(block) {
        this.entity = if(block is BlockEntityProvider){
            BlockEntityType.Builder.create(Supplier { block.createBlockEntity(null) }, block).build(null) as BlockEntityType<T>
        }else null
        this.renderer = blockEntityRenderer as KClass<BlockEntityRenderer<T>>
        this.container = blockEntityScreenHandler as KClass<ScreenHandler>
        this.containerScreen = blockEntityScreen as KClass<HandledScreen<*>>
    }

    constructor(block: Block, blockEntityScreenHandler: KClass<*>, blockEntityScreen: KClass<*>) : super(block) {
        this.entity = if(block is BlockEntityProvider){
            BlockEntityType.Builder.create(Supplier { block.createBlockEntity(null) }, block).build(null) as BlockEntityType<T>
        }else null
        this.container = blockEntityScreenHandler as KClass<ScreenHandler>
        this.containerScreen = blockEntityScreen as KClass<HandledScreen<*>>
    }

    override fun init(identifier: Identifier) {
        super.init(identifier)
        if (entity != null) {
            Registry.register(Registry.BLOCK_ENTITY_TYPE, identifier, entity)
        }
        if (container != null) {
            ContainerProviderRegistry.INSTANCE.registerFactory(identifier) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
                val pos = packetByteBuf.readBlockPos()
                if(container!!.java.constructors[0].parameterTypes[2] == Network::class.java) {
                    val tag = packetByteBuf.readCompoundTag()
                    val network = Network(null, playerEntity.world);
                    network.fromTag(tag!!)
                    container!!.java.constructors[0].newInstance(syncId,
                        playerEntity.inventory,
                        network,
                        ScreenHandlerContext.create(playerEntity.world, pos)
                    ) as ScreenHandler
                }else{
                    container!!.java.constructors[0].newInstance(syncId,
                        playerEntity.inventory,
                        playerEntity.world.getBlockEntity(pos),
                        ScreenHandlerContext.create(playerEntity.world, pos)
                    ) as ScreenHandler
                }
            }
        }
    }

    override fun initClient(identifier: Identifier) {
        super.initClient(identifier)
        if(containerScreen != null) {
            ScreenProviderRegistry.INSTANCE.registerFactory(identifier) { syncId: Int, _, playerEntity: PlayerEntity, packetByteBuf: PacketByteBuf ->
                val pos = packetByteBuf.readBlockPos()
                if(container!!.java.constructors[0].parameterTypes[2] == Network::class.java) {
                    val tag = packetByteBuf.readCompoundTag()
                    val network = Network(null, playerEntity.world);
                    network.fromTag(tag!!)
                    containerScreen!!.java.constructors[0].newInstance(
                        container!!.java.constructors[0].newInstance(
                            syncId,
                            playerEntity.inventory,
                            network,
                            ScreenHandlerContext.EMPTY
                        ) as ScreenHandler, playerEntity.inventory, TranslatableText("screen.${MOD_ID}.${getBlockId(block)?.path}")
                    ) as HandledScreen<*>
                }else{
                    val entity = playerEntity.entityWorld.getBlockEntity(pos)
                    containerScreen!!.java.constructors[0].newInstance(
                        container!!.java.constructors[0].newInstance(
                            syncId,
                            playerEntity.inventory,
                            entity,
                            ScreenHandlerContext.EMPTY
                        ) as ScreenHandler, playerEntity.inventory, TranslatableText("screen.${MOD_ID}.${getBlockId(block)?.path}")
                    ) as HandledScreen<*>
                }
            }
        }
        if(renderer != null) {
            BlockEntityRendererRegistry.INSTANCE.register(entity) { it2 ->
                renderer!!.java.constructors[0].newInstance(it2) as BlockEntityRenderer<T>
            }
        }
    }
}