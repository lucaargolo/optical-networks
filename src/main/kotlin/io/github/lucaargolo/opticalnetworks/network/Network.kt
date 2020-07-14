package io.github.lucaargolo.opticalnetworks.network

import io.github.lucaargolo.opticalnetworks.blocks.`interface`.Interface
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.InterfaceBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.assembler.Assembler
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.controller.Controller
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.crafting.CraftingComputer
import io.github.lucaargolo.opticalnetworks.blocks.crafting.CraftingComputerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.DriveRack
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.DriveRackBlockEntity
import io.github.lucaargolo.opticalnetworks.items.basic.ItemDrive
import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import io.github.lucaargolo.opticalnetworks.utils.areStacksCompatible
import io.github.lucaargolo.opticalnetworks.utils.autocrafting.CraftingAction
import io.github.lucaargolo.opticalnetworks.utils.getStackFromTag
import io.github.lucaargolo.opticalnetworks.utils.getTagFromStack
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.LongTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import java.util.*

class Network private constructor(val state: NetworkState?, var world: World) {

    var id: UUID = UUID.randomUUID()
    var components = linkedSetOf<BlockPos>()
    var componentsMap = linkedMapOf<BlockPos, Block>()
    var processingActions = linkedMapOf<CraftingAction, BlockPos>()
    var processingMachines = linkedSetOf<BlockPos>()
    var processingItems = linkedMapOf<MutableList<ItemStack>, BlockPos>()
    var controllerList = mutableListOf<BlockPos>()

    fun addComponent(blockPos: BlockPos, block: Block) {
        if(block is Controller) {
            controllerList.add(blockPos)
        }
        components.add(blockPos)
        componentsMap[blockPos] = block
        state!!.recreateNetwork(this)
    }

    fun removeComponent(blockPos: BlockPos) {
        if(controllerList.contains(blockPos)) {
            controllerList.remove(blockPos)
        }
        components.remove(blockPos)
        componentsMap.remove(blockPos)
        state!!.recreateNetwork(this)
    }

    fun getCraftableInfo(itemStack: ItemStack): Pair<CompoundTag, BlockPos>? {
        componentsMap.forEach {
            if(it.value is Assembler) {
                val be = world.getBlockEntity(it.key)
                if(be is AssemblerBlockEntity) {
                    val invStack = be.inventory[0]
                    if (be.isBlueprintValid()) {
                        val stack = ItemStack.fromTag(invStack.tag!!.getCompound("output"))
                        if(areStacksCompatible(itemStack, stack)) return Pair(invStack.tag!!, it.key)
                    }
                }
            }
            if(it.value is Interface) {
                val be = world.getBlockEntity(it.key)
                if(be is InterfaceBlockEntity) {
                    (0 until be.size()).forEach { idx ->
                        val invStack = be.getStack(idx)
                        if(!invStack.isEmpty) {
                            val outputTag = invStack.tag!!.get("output")
                            if(outputTag is CompoundTag) {
                                val stack = ItemStack.fromTag(outputTag)
                                if(areStacksCompatible(itemStack, stack)) return Pair(invStack.tag!!, it.key)
                            }
                            if(outputTag is ListTag) {
                                outputTag.forEach { tag ->
                                    val stack = ItemStack.fromTag(tag as CompoundTag)
                                    if(areStacksCompatible(itemStack, stack)) return Pair(invStack.tag!!, it.key)
                                }
                            }
                        }
                    }

                }
            }
        }
        return null
    }

    fun getAvailableCraftables(search: String): MutableList<ItemStack> {
        val itemMemory = mutableSetOf<Item>()
        val itemList = mutableListOf<ItemStack>()
        componentsMap.forEach { it ->
            if(it.value is Assembler) {
                val be = world.getBlockEntity(it.key)
                if(be is AssemblerBlockEntity) {
                    val invStack = be.inventory[0]
                    if (be.isBlueprintValid()) {
                        val stack = ItemStack.fromTag(invStack.tag!!.getCompound("output"))
                        val item = stack.item
                        if (TranslatableText(stack.item.translationKey).toString().toLowerCase().contains(search)) {
                            if (itemMemory.contains(item)) {
                                var foundEqual = false
                                itemList.forEach {
                                    if (areStacksCompatible(it, stack)) {
                                        it.increment(stack.count)
                                        foundEqual = true
                                    }
                                }
                                if (!foundEqual) {
                                    itemList.add(stack)
                                }
                            } else {
                                itemList.add(stack)
                                itemMemory.add(item)
                            }
                        }
                    }
                }
            }
            if(it.value is Interface) {
                val be = world.getBlockEntity(it.key)
                if(be is InterfaceBlockEntity) {
                    (0 until be.size()).forEach {
                        val invStack = be.getStack(it)
                        if(!invStack.isEmpty) {
                            val outputTag = invStack.tag!!.get("output")
                            if(outputTag is CompoundTag) {
                                val stack = ItemStack.fromTag(outputTag)
                                val item = stack.item
                                if (TranslatableText(stack.item.translationKey).toString().toLowerCase().contains(search)) {
                                    if (itemMemory.contains(item)) {
                                        var foundEqual = false
                                        itemList.forEach {
                                            if (areStacksCompatible(it, stack)) {
                                                it.increment(stack.count)
                                                foundEqual = true
                                            }
                                        }
                                        if (!foundEqual) {
                                            itemList.add(stack)
                                        }
                                    } else {
                                        itemList.add(stack)
                                        itemMemory.add(item)
                                    }
                                }
                            }
                            if(outputTag is ListTag) {
                                outputTag.forEach { ctag ->
                                    val stack = ItemStack.fromTag(ctag as CompoundTag)
                                    val item = stack.item
                                    if (TranslatableText(stack.item.translationKey).toString().toLowerCase().contains(search)) {
                                        if (itemMemory.contains(item)) {
                                            var foundEqual = false
                                            itemList.forEach {
                                                if (areStacksCompatible(it, stack)) {
                                                    it.increment(stack.count)
                                                    foundEqual = true
                                                }
                                            }
                                            if (!foundEqual) {
                                                itemList.add(stack)
                                            }
                                        } else {
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
        }
        return itemList
    }

    fun getAvailableStacks(search: String): MutableList<ItemStack> {
        val itemMemory = mutableSetOf<Item>()
        val itemList = mutableListOf<ItemStack>()
        componentsMap.forEach { it ->
            if(it.value is DriveRack) {
                val be = world.getBlockEntity(it.key)
                if(be is DriveRackBlockEntity) {
                    be.inventory.forEach { invStack ->
                        if (invStack.item is ItemDrive) {
                            val stackTag = invStack.orCreateTag
                            if (stackTag.contains("items")) {
                                val itemsTag = stackTag.get("items") as ListTag
                                itemsTag.forEach { itemTag ->
                                    val stack = getStackFromTag(itemTag as CompoundTag)
                                    val item = stack.item
                                    if (TranslatableText(stack.item.translationKey).toString().toLowerCase().contains(search)) {
                                        if (itemMemory.contains(item)) {
                                            var foundEqual = false
                                            itemList.forEach {
                                                if (areStacksCompatible(it, stack)) {
                                                    it.increment(stack.count)
                                                    foundEqual = true
                                                }
                                            }
                                            if (!foundEqual) {
                                                itemList.add(stack)
                                            }
                                        } else {
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
        }
        return itemList
    }

    fun getController(): BlockPos? {
        return if(controllerList.size == 1) controllerList[0]
        else null
    }

    fun isValid() = (controllerList.size == 1) && (world.getBlockEntity(controllerList[0]) is ControllerBlockEntity)

    fun updateColor() {
        val controllerBlockEntity = getController()?.let{ world.getBlockEntity(it) }
        val color = if(controllerBlockEntity is ControllerBlockEntity) controllerBlockEntity.currentColor else null
        color?.let {
            components.forEach { p ->
                val be = world.getBlockEntity(p)
                if(be is NetworkBlockEntity) {
                    be.currentColor = color
                    be.sync()
                }
            }
        }
    }

    fun getCraftingSpace(): Pair<Int, Int> {
        var totalSpace = 0
        var usedSpace = 0;
        getCraftingCpus().forEach { craftingComputerEntity ->
            val pair = craftingComputerEntity.getSpace()
            usedSpace += pair.first
            totalSpace += pair.second
        }
        return Pair(usedSpace, totalSpace)
    }

    fun getCraftingCpus(): List<CraftingComputerBlockEntity> {
        val craftingCpuList = mutableListOf<CraftingComputerBlockEntity>()
        componentsMap.forEach {
            if(it.value is CraftingComputer) {
                craftingCpuList.add(world.getBlockEntity(it.key) as CraftingComputerBlockEntity)
            }
        }
        return craftingCpuList
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
                if(rackStack.item is ItemDrive) {
                    totalSpace += (rackStack.item as ItemDrive).space
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

    fun removeStack(stack: ItemStack): ItemStack {
        getStorageByPriority().forEach {rackEntity ->
            rackEntity.inventory.forEach {rackStack ->
                if(rackStack.item is ItemDrive) {
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
                            if(!currentStack.isEmpty) newItemsTag.add(
                                getTagFromStack(currentStack)
                            )
                        }
                        stackTag.put("items", newItemsTag)
                    }
                }
            }
            rackEntity.markDirty()
            if(rackEntity.world?.isClient == false) (rackEntity as BlockEntityClientSerializable).sync()
        }
        return stack
    }

    fun insertStack(stack: ItemStack): ItemStack {
        val copyStack = stack.copy()
        getStorageByPriority().forEach {rackEntity ->
            rackEntity.inventory.forEach {rackStack ->
                if(rackStack.item is ItemDrive && !stack.isEmpty) {
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
                        if(totalCount < (rackStack.item as ItemDrive).space) {
                            val availableBytes = (rackStack.item as ItemDrive).space - totalCount
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
            rackEntity.markDirty()
            if(rackEntity.world?.isClient == false) (rackEntity as BlockEntityClientSerializable).sync()
        }
        if(!ItemStack.areEqual(copyStack, stack)) {
            val processingItemsIterator = processingItems.iterator()
            while(processingItemsIterator.hasNext()) {
                val processingItemsEntry = processingItemsIterator.next()
                val pendingMachine = processingItemsEntry.value
                val pendingStacks = processingItemsEntry.key
                val pendingStacksIterator = pendingStacks.iterator()
                while(pendingStacksIterator.hasNext()) {
                    val pendingStack = pendingStacksIterator.next()
                    if(areStacksCompatible(copyStack, pendingStack)) {
                        val inserted = copyStack.count - stack.count
                        pendingStack.decrement(inserted)
                        if(pendingStack.isEmpty)
                            pendingStacksIterator.remove()
                    }
                }
                if(pendingStacks.isEmpty()) {
                    processingItemsIterator.remove()
                    processingMachines.remove(pendingMachine)
                }
            }
        }
        return stack;
    }

    fun toTag(tag: CompoundTag): CompoundTag {

        tag.putUuid("id", id)
        val controllersTag = ListTag()
        tag.put("controllers", controllersTag)
        controllerList.forEach {
            controllersTag.add(LongTag.of(it.asLong()))
        }
        val componentsTag = ListTag()
        componentsMap.forEach {
            val componentTag = CompoundTag()
            componentTag.putString("id", Registry.BLOCK.getId(it.value).toString())
            componentTag.putLong("pos", it.key.asLong())
            componentsTag.add(componentTag)
        }
        tag.put("components", componentsTag)
        return tag;

    }

    companion object {

        fun create(state: NetworkState, world: ServerWorld): Network {
            return Network(state, world)
        }

        fun fromTag(tag: CompoundTag, world: World): Network {
            val network = Network(null, world)
            network.components = linkedSetOf()
            network.componentsMap = linkedMapOf()
            network.controllerList = mutableListOf()
            network.id = tag.getUuid("id")
            val controllersTag = tag.get("controllers") as ListTag
            controllersTag.forEach {
                val pos = BlockPos.fromLong((it as LongTag).long)
                if(world.getBlockEntity(pos) is ControllerBlockEntity)
                    network.controllerList.add(pos)
            }
            val componentsTag = tag.get("components") as ListTag
            componentsTag.forEach {
                val itTag = it as CompoundTag
                val pos = BlockPos.fromLong(itTag.getLong("pos"))
                val block = Registry.BLOCK.get(Identifier(itTag.getString("id")))
                network.components.add(pos)
                network.componentsMap[pos] = block
            }
            return network
        }

    }



}