package io.github.lucaargolo.opticalnetworks.network

import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.DriveRack
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.DriveRackBlockEntity
import io.github.lucaargolo.opticalnetworks.items.basic.DiscDrive
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import java.awt.Color
import java.util.*

class Network(val state: NetworkState?, var world: World) {

    var id: UUID = UUID.randomUUID()
    var components = mutableSetOf<BlockPos>()
    var componentsMap = mutableMapOf<BlockPos, Block>()
    var controller: BlockPos? = null

    fun addComponent(blockPos: BlockPos, block: Block) {
        components.add(blockPos)
        componentsMap[blockPos] = block
        state!!.updateNetwork(this)
    }

    fun removeComponent(blockPos: BlockPos) {
        components.remove(blockPos)
        componentsMap.remove(blockPos)
        state!!.updateNetwork(this)
    }

    fun searchStacks(string: String): MutableList<ItemStack> {
        val itemMemory = mutableSetOf<Item>()
        val itemList = mutableListOf<ItemStack>()
        componentsMap.forEach { it ->
            if(it.value is DriveRack) {
                (world.getBlockEntity(it.key) as DriveRackBlockEntity).inventory.forEach { invStack ->
                    if(invStack.item is DiscDrive) {
                        val stackTag = invStack.orCreateTag
                        if(stackTag.contains("items")) {
                            val itemsTag = stackTag.get("items") as ListTag
                            itemsTag.forEach {itemTag ->
                                val stack = getStackFromTag(itemTag as CompoundTag)
                                val item = stack.item
                                if(TranslatableText(stack.item.translationKey).toString().toLowerCase().contains(string)) {
                                    if(itemMemory.contains(item)) {
                                        var foundEqual = false
                                        itemList.forEach {
                                            if(areStacksCompatible(it, stack)) {
                                                it.increment(stack.count)
                                                foundEqual = true
                                            }
                                        }
                                        if(!foundEqual) {
                                            itemList.add(stack)
                                        }
                                    }else{
                                        itemList.add(stack)
                                        itemMemory.add(item)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return itemList
    }

    fun updateColor() {
        components.forEach { p ->
            world.players.forEach { pl ->
                val passedData = PacketByteBuf(Unpooled.buffer())
                passedData.writeBlockPos(p)
                passedData.writeInt(getColor().rgb)
                ServerSidePacketRegistry.INSTANCE.sendToPlayer(pl, UPDATE_COLOR_MAP_S2C_PACKET, passedData)
            }
        }
    }

    private fun getColor(): Color {
        val be = controller?.let{ world.getBlockEntity(it) }
        if(be is ControllerBlockEntity) {
            return be.storedColor
        }
        return Color.BLACK
    }

    private fun getStorageByPriority(): List<DriveRackBlockEntity> {
        val driveRacksList = mutableListOf<DriveRackBlockEntity>()
        componentsMap.forEach {
            if(it.value is DriveRack) {
                driveRacksList.add(world.getBlockEntity(it.key) as DriveRackBlockEntity)
            }
        }
        return driveRacksList.sortedBy {it.priority}.reversed()
    }

    fun getSpace(): Pair<Int, Int> {
        var totalSpace = 0
        var usedSpace = 0;
        getStorageByPriority().forEach {rackEntity ->
            rackEntity.inventory.forEach {rackStack ->
                if(rackStack.item is DiscDrive) {
                    totalSpace += (rackStack.item as DiscDrive).bytes
                    val stackTag = rackStack.orCreateTag
                    if(stackTag.contains("items")) {
                        val itemsTag = stackTag.get("items") as ListTag
                        itemsTag.forEach {
                            usedSpace += getStackFromTag(it as CompoundTag).count
                        }
                    }
                }
            }
        }
        return Pair(usedSpace, totalSpace)
    }

    fun removeStack(stack: ItemStack) {
        getStorageByPriority().forEach {rackEntity ->
            rackEntity.inventory.forEach {rackStack ->
                if(rackStack.item is DiscDrive) {
                    val stackTag = rackStack.orCreateTag
                    if(stackTag.contains("items") && stack.count > 0) {
                        val itemsTag = stackTag.get("items") as ListTag
                        val newItemsTag = ListTag()
                        itemsTag.forEach {
                            var currentStack = getStackFromTag(it as CompoundTag)
                            if(areStacksCompatible(currentStack, stack)) {
                                if(currentStack.count < stack.count) {
                                    stack.decrement(currentStack.count)
                                    currentStack = ItemStack.EMPTY
                                }else{
                                    currentStack.decrement(stack.count)
                                    stack.decrement(stack.count)
                                }
                            }
                            if(!currentStack.isEmpty) newItemsTag.add(getTagFromStack(currentStack))
                        }
                        stackTag.put("items", newItemsTag)
                    }
                }
            }
            (rackEntity as BlockEntityClientSerializable).sync()
        }
    }

    fun insertStack(stack: ItemStack): ItemStack {
        getStorageByPriority().forEach {rackEntity ->
            rackEntity.inventory.forEach {rackStack ->
                if(rackStack.item is DiscDrive && !stack.isEmpty) {
                    val stackTag = rackStack.orCreateTag
                    if(!stackTag.contains("items")) {
                        val itemsTag = ListTag()
                        val itemTag = getTagFromStack(stack)
                        itemsTag.add(itemTag)
                        stackTag.put("items", itemsTag)
                        stack.decrement(stack.count)
                    }else{
                        val itemsTag = stackTag.get("items") as ListTag
                        var totalCount = 0;
                        itemsTag.forEach {
                            totalCount += getStackFromTag(it as CompoundTag).count
                        }
                        if(totalCount < (rackStack.item as DiscDrive).bytes) {
                            val availableBytes = (rackStack.item as DiscDrive).bytes - totalCount
                            if(stack.count > availableBytes) {
                                var added = false
                                itemsTag.forEach {
                                    if(areStacksCompatible(getStackFromTag(it as CompoundTag), stack)) {
                                        it.putInt("Count", it.getInt("Count")+availableBytes)
                                        added = true
                                    }
                                }
                                if(!added) {
                                    val itemTag = getTagFromStack(stack)
                                    itemTag.putInt("Count", availableBytes)
                                    itemsTag.add(itemTag)
                                }
                                stack.count -= availableBytes
                            }else {
                                var added = false
                                itemsTag.forEach {
                                    if(areStacksCompatible(getStackFromTag(it as CompoundTag), stack)) {
                                        it.putInt("Count", it.getInt("Count")+stack.count)
                                        added = true
                                    }
                                }
                                if(!added) {
                                    val itemTag = getTagFromStack(stack)
                                    itemTag.putInt("Count", stack.count)
                                    itemsTag.add(itemTag)
                                }
                                stack.decrement(stack.count)
                            }
                        }
                    }
                }
            }
            (rackEntity as BlockEntityClientSerializable).sync()
        }
        return stack;
    }

    fun toTag(tag: CompoundTag): CompoundTag {
        val componentsTag = ListTag()
        tag.putUuid("id", id)
        tag.putLong("controller", controller?.asLong() ?: 0L)
        componentsMap.forEach {
            val componentTag = CompoundTag()
            componentTag.putString("id", Registry.BLOCK.getId(it.value).toString())
            componentTag.putLong("pos", it.key.asLong())
            componentsTag.add(componentTag)
        }
        tag.put("components", componentsTag)
        return tag;
    }

    fun fromTag(tag: CompoundTag) {
        components = mutableSetOf()
        componentsMap = mutableMapOf()
        id = tag.getUuid("id")
        val controllerPos = BlockPos.fromLong(tag.getLong("controller"))
        if(world.getBlockEntity(controllerPos) is ControllerBlockEntity) {
            controller = controllerPos
        }
        val componentsTag = tag.get("components") as ListTag
        componentsTag.forEach {
            val itag = it as CompoundTag
            val pos = BlockPos.fromLong(itag.getLong("pos"))
            val block = Registry.BLOCK.get(Identifier(itag.getString("id")))
            components.add(pos)
            componentsMap[pos] = block
        }
    }

}