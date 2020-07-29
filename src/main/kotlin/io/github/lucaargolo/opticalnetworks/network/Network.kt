package io.github.lucaargolo.opticalnetworks.network

import io.github.lucaargolo.opticalnetworks.blocks.CONTROLLER
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.Interface
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.InterfaceBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.assembler.Assembler
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.crafting.CraftingComputer
import io.github.lucaargolo.opticalnetworks.blocks.crafting.CraftingComputerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.DriveRack
import io.github.lucaargolo.opticalnetworks.blocks.drive_rack.DriveRackBlockEntity
import io.github.lucaargolo.opticalnetworks.items.basic.ItemDrive
import io.github.lucaargolo.opticalnetworks.network.blocks.CableConnectable
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
import net.minecraft.nbt.StringTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.math.min


class Network private constructor(val state: NetworkState, var world: World, val type: Type) {

    enum class Type {
        COMPONENTS,
        CONTROLLER
    }

    var id: UUID = UUID.randomUUID()
    var components = linkedSetOf<BlockPos>()
    var componentsMap = linkedMapOf<BlockPos, Block>()

    var controllerNetworks: LinkedHashSet<UUID> = linkedSetOf()
    var componentNetworks: LinkedHashSet<UUID> = linkedSetOf()

    var storedPower = 0.0
        get() {
            return if(this.type == Type.COMPONENTS)
                getControllerNetwork()?.storedPower ?: field
            else field
        }
        set(value) {
            if(this.type == Type.COMPONENTS)
                getControllerNetwork()?.storedPower = value
            else {
                field = value
                (world.getBlockEntity(mainController) as? ControllerBlockEntity)?.let{
                    it.networkStoredPowerCache = value
                    it.markDirty()
                    it.sync()
                }
            }
        }

    private var processingActions = linkedMapOf<CraftingAction, BlockPos>()
    
    fun getProcessingActions(): LinkedHashMap<CraftingAction, BlockPos> {
        return if(type == Type.COMPONENTS) {
            getControllerNetwork()?.getProcessingActions() ?: linkedMapOf()
        }else{
            val finalHashMap = linkedMapOf<CraftingAction, BlockPos>()
            componentNetworks.forEach { id ->
                state.networks[id]?.processingActions?.let { finalHashMap.putAll(it) }
            }
            finalHashMap
        }
    }

    fun addProcessingAction(action: CraftingAction, pos: BlockPos) {
        processingActions[action] = pos
    }

    fun removeProcessingAction(action: CraftingAction) {
        getProcessingMachines().forEach { (pos, act) ->
            if(act == action) removeProcessingMachine(pos)
        }
        if(processingActions.remove(action) == null && getControllerNetwork()?.processingActions?.remove(action) == null) {
            getControllerNetwork()?.componentNetworks?.forEach {
                if(state.networks[it]?.processingActions?.remove(action) != null)
                    return@forEach
            }
        }
    }
    
    private var processingMachines = linkedMapOf<BlockPos, CraftingAction>()

    fun getProcessingMachines(): LinkedHashMap<BlockPos, CraftingAction> {
        return if(type == Type.COMPONENTS) {
            getControllerNetwork()?.getProcessingMachines() ?: linkedMapOf()
        }else{
            val finalHashMap = linkedMapOf<BlockPos, CraftingAction>()
            componentNetworks.forEach { id ->
                state.networks[id]?.processingMachines?.let { finalHashMap.putAll(it) }
            }
            finalHashMap
        }
    }
    
    fun addProcessingMachine(pos: BlockPos, action: CraftingAction) {
        processingMachines[pos] = action
    }
    
    fun removeProcessingMachine(pos: BlockPos) {
        getProcessingItems().forEach { (list, processingPos) ->
            if(processingPos == pos) removeProcessingItem(list)
        }
        if(processingMachines.remove(pos) == null && getControllerNetwork()?.processingMachines?.remove(pos) == null) {
            getControllerNetwork()?.componentNetworks?.forEach {
                if(state.networks[it]?.processingMachines?.remove(pos) != null)
                    return@forEach
            }
        }
    }
    
    private var processingItems = linkedMapOf<MutableList<ItemStack>, BlockPos>()

    fun getProcessingItems(): LinkedHashMap<MutableList<ItemStack>, BlockPos> {
        return if(type == Type.COMPONENTS) {
            getControllerNetwork()?.getProcessingItems() ?: linkedMapOf()
        }else{
            val finalHashMap = linkedMapOf<MutableList<ItemStack>, BlockPos>()
            componentNetworks.forEach { id ->
                state.networks[id]?.processingItems?.let { finalHashMap.putAll(it) }
            }
            finalHashMap
        }
    }

    fun addProcessingItem(list: MutableList<ItemStack>, pos: BlockPos) {
        processingItems[list] = pos
    }

    fun removeProcessingItem(list: MutableList<ItemStack>){
        if(processingItems.remove(list) == null && getControllerNetwork()?.processingItems?.remove(list) == null) {
            getControllerNetwork()?.componentNetworks?.forEach {
                if(state.networks[it]?.processingItems?.remove(list) != null)
                    return@forEach
            }
        }
    }
    
    var mainController: BlockPos = BlockPos.ORIGIN
    private fun getControllerNetwork(): Network? {
        val iterator = controllerNetworks.iterator()
        return if(iterator.hasNext()) state.networks[iterator.next()]
        else null
    }
    fun getController(): BlockPos? {
        return when {
            type == Type.CONTROLLER -> mainController
            controllerNetworks.size == 1 -> getControllerNetwork()?.getController()
            else -> null
        }
    }

    fun getMaxStoredPower(): Double {
        if(type == Type.COMPONENTS) {
            return getControllerNetwork()?.getMaxStoredPower() ?: 0.0
        }else{
            var maxStoredPower = MAX_STORED_POWER
            componentsMap.forEach { (pos, block) ->
                if(block == CONTROLLER && pos != mainController && world.getBlockEntity(pos) is ControllerBlockEntity)
                    maxStoredPower += MAX_STORED_POWER
            }
            return maxStoredPower
        }
    }

    fun isValid(): Boolean {
        return ((type == Type.COMPONENTS && controllerNetworks.size == 1 && getControllerNetwork()?.isValid() == true) || (type == Type.CONTROLLER && world.getBlockEntity(mainController) is ControllerBlockEntity)) && storedPower >= 128.0
    }

    fun getBandwidthStats(): Pair<Double, Double> {
        var currentUsage = 0.0
        componentsMap.forEach { (block, _) -> currentUsage += (block as? CableConnectable)?.bandwidthUsage ?: 0.0 }
        val overflowPercentage = ((min(0.0, currentUsage - 1000.0)/100)*2)
        if(overflowPercentage != 0.0) return Pair(currentUsage, 1/overflowPercentage)
        return Pair(currentUsage, 0.0)
    }

    fun addComponent(blockPos: BlockPos, block: Block) {
        components.add(blockPos)
        componentsMap[blockPos] = block
        state.recreateNetwork(this)
    }

    fun removeComponent(blockPos: BlockPos) {
        components.remove(blockPos)
        componentsMap.remove(blockPos)
        state.recreateNetwork(this)
    }

    fun getCraftableInfo(itemStack: ItemStack): Pair<CompoundTag, BlockPos>? {
        if(type == Type.COMPONENTS) {
            return getControllerNetwork()?.getCraftableInfo(itemStack)
        }else{
            componentNetworks.forEach { networkUUID ->
                val network = state.networks[networkUUID]
                network?.componentsMap?.forEach {
                    if(it.value is Assembler) {
                        val be = network.world.getBlockEntity(it.key)
                        if(be is AssemblerBlockEntity) {
                            val invStack = be.inventory[0]
                            if (be.isBlueprintValid()) {
                                val stack = ItemStack.fromTag(invStack.tag!!.getCompound("output"))
                                if(areStacksCompatible(itemStack, stack)) return Pair(invStack.tag!!, it.key)
                            }
                        }
                    }
                    if(it.value is Interface) {
                        val be = network.world.getBlockEntity(it.key)
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
            }
        }
        return null
    }

    fun getAvailableCraftables(search: String): MutableList<ItemStack> {
        if(!this.isValid()) return mutableListOf()
        if(type == Type.COMPONENTS) {
            return getControllerNetwork()?.getAvailableCraftables(search) ?: mutableListOf()
        }else{
            val itemMemory = mutableSetOf<Item>()
            val itemList = mutableListOf<ItemStack>()
            componentNetworks.forEach { networkUUID ->
                val network = state.networks[networkUUID]
                network?.componentsMap?.forEach { it ->
                    if (it.value is Assembler) {
                        val be = world.getBlockEntity(it.key)
                        if (be is AssemblerBlockEntity) {
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
                    if (it.value is Interface) {
                        val be = world.getBlockEntity(it.key)
                        if (be is InterfaceBlockEntity) {
                            (0 until be.size()).forEach {
                                val invStack = be.getStack(it)
                                if (!invStack.isEmpty) {
                                    val outputTag = invStack.tag!!.get("output")
                                    if (outputTag is CompoundTag) {
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
                                    if (outputTag is ListTag) {
                                        outputTag.forEach { ctag ->
                                            val stack = ItemStack.fromTag(ctag as CompoundTag)
                                            val item = stack.item
                                            if (TranslatableText(stack.item.translationKey).toString().toLowerCase()
                                                    .contains(search)
                                            ) {
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
            }
            return itemList
        }
    }

    fun getAvailableStacks(search: String): MutableList<ItemStack> {
        if(!this.isValid()) return mutableListOf()
        if(type == Type.COMPONENTS) {
            return getControllerNetwork()?.getAvailableStacks(search) ?: mutableListOf()
        }else {
            val itemMemory = mutableSetOf<Item>()
            val itemList = mutableListOf<ItemStack>()
            componentNetworks.forEach { networkUUID ->
                val network = state.networks[networkUUID]
                network?.componentsMap?.forEach { it ->
                    if (it.value is DriveRack) {
                        val be = world.getBlockEntity(it.key)
                        if (be is DriveRackBlockEntity) {
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
            }
            return itemList
        }
    }

    fun updateColor() {
        val controllerBlockEntity = getController()?.let{ world.getBlockEntity(it) }
        val color = if(controllerBlockEntity is ControllerBlockEntity) controllerBlockEntity.currentColor else null
        color?.let {
            components.forEach { p ->
                val be = world.getBlockEntity(p)
                if(be is NetworkBlockEntity) {
                    be.currentColor = color
                    be.markDirty()
                    be.sync()
                }
            }
            componentNetworks.forEach {
                state.networks[it]?.updateColor()
            }
        }
    }

    fun getCraftingSpace(): Pair<Int, Int> {
        var totalSpace = 0
        var usedSpace = 0
        getCraftingCpus().forEach { craftingComputerEntity ->
            val pair = craftingComputerEntity.getSpace()
            usedSpace += pair.first
            totalSpace += pair.second
        }
        return Pair(usedSpace, totalSpace)
    }

    fun getCraftingCpus(): List<CraftingComputerBlockEntity> {
        return if(type == Type.COMPONENTS) {
            getControllerNetwork()?.getCraftingCpus() ?: listOf()
        }else{
            val craftingCpuList = mutableListOf<CraftingComputerBlockEntity>()
            componentNetworks.forEach { networkUUID ->
                val network = state.networks[networkUUID]
                network?.componentsMap?.forEach {
                    if(it.value is CraftingComputer) {
                        craftingCpuList.add(world.getBlockEntity(it.key) as CraftingComputerBlockEntity)
                    }
                }
            }
            craftingCpuList
        }
    }

    fun getSpace(): Pair<Int, Int> {
        var totalSpace = 0
        var usedSpace = 0
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

    private fun getStorageByPriority(): List<DriveRackBlockEntity> {
        if(type == Type.COMPONENTS) {
            return getControllerNetwork()?.getStorageByPriority() ?: listOf()
        }else{
            val driveRacksList = mutableListOf<DriveRackBlockEntity>()
            componentNetworks.forEach { networkUUID ->
                val network = state.networks[networkUUID]
                network?.componentsMap?.forEach {
                    if(it.value is DriveRack) {
                        driveRacksList.add(world.getBlockEntity(it.key) as DriveRackBlockEntity)
                    }
                }
            }
            return driveRacksList.sortedBy {it.priority}.reversed()
        }
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
                        var totalCount = 0
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
            val processingItemsIterator = getProcessingItems().iterator()
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
                    removeProcessingMachine(pendingMachine)
                }
            }
        }
        return stack
    }

    fun floodFillNetworkIds(network: Network, uuidSet: LinkedHashSet<UUID>): LinkedHashSet<UUID> {
        if(uuidSet.add(network.id)) {
            network.componentNetworks.forEach {
                state.networks[it]?.let { uuidSet.addAll(floodFillNetworkIds(it, uuidSet)) }
            }
            network.controllerNetworks.forEach {
                state.networks[it]?.let { uuidSet.addAll(floodFillNetworkIds(it, uuidSet)) }
            }
        }
        return uuidSet
    }

    fun getOptimizedStateTag(tag: CompoundTag): CompoundTag {
        val uuidSet = floodFillNetworkIds(this, linkedSetOf())
        state.networks.forEach { (uuid, network) ->
            if(uuidSet.contains(uuid)) tag.put(uuid.toString(), network.toTag(CompoundTag()))
        }
        return tag
    }

    fun toTag(tag: CompoundTag): CompoundTag {
        tag.putInt("type", Type.values().indexOf(type))
        tag.putDouble("storedPower", storedPower)
        tag.putLong("mainController", mainController.asLong())
        tag.putUuid("id", id)
        val controllerNetworksTag = ListTag()
        controllerNetworks.forEach {
            controllerNetworksTag.add(StringTag.of(it.toString()))
        }
        tag.put("controllerNetworks", controllerNetworksTag)
        val componentNetworksTag = ListTag()
        componentNetworks.forEach {
            componentNetworksTag.add(StringTag.of(it.toString()))
        }
        tag.put("componentNetworks", componentNetworksTag)
        val componentsTag = ListTag()
        componentsMap.forEach {
            val componentTag = CompoundTag()
            componentTag.putString("id", Registry.BLOCK.getId(it.value).toString())
            componentTag.putLong("pos", it.key.asLong())
            componentsTag.add(componentTag)
        }
        tag.put("components", componentsTag)
        return tag

    }

    companion object {

        const val MAX_STORED_POWER = 100000.0

        fun create(state: NetworkState, world: ServerWorld, type: Type): Network {
            return Network(state, world, type)
        }

        fun fromTag(state: NetworkState, world: World, tag: CompoundTag): Network {
            val type = Type.values()[tag.getInt("type")]
            val network = Network(state, world, type)
            network.storedPower = tag.getDouble("storedPower")
            network.id = tag.getUuid("id")
            network.mainController = BlockPos.fromLong(tag.getLong("mainController"))
            val controllerNetworksTag = tag.get("controllerNetworks") as ListTag
            controllerNetworksTag.forEach {
                network.controllerNetworks.add(UUID.fromString((it as StringTag).asString()))
            }
            val componentNetworksTag = tag.get("componentNetworks") as ListTag
            componentNetworksTag.forEach {
                network.componentNetworks.add(UUID.fromString((it as StringTag).asString()))
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