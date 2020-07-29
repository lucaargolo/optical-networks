package io.github.lucaargolo.opticalnetworks.blocks.crafting

import io.github.lucaargolo.opticalnetworks.blocks.`interface`.Interface
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.InterfaceBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerBlockEntity
import io.github.lucaargolo.opticalnetworks.items.basic.CraftingMemory
import io.github.lucaargolo.opticalnetworks.items.basic.CraftingProcessingUnit
import io.github.lucaargolo.opticalnetworks.utils.areStacksCompatible
import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import io.github.lucaargolo.opticalnetworks.utils.autocrafting.CraftingAction
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ChestBlock
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.state.property.Properties
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class CraftingComputerBlockEntity(block: Block): NetworkBlockEntity(block), SidedInventory {

    var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(4, ItemStack.EMPTY)

    fun getCraftingCores(): Int {
        return if(inventory[0].item !is CraftingProcessingUnit) 0 else (inventory[0].item as CraftingProcessingUnit).cores
    }

    fun getCraftingSpeed(): Float {
        return if(inventory[0].item !is CraftingProcessingUnit) 0.01F else (inventory[0].item as CraftingProcessingUnit).speed
    }

    fun getSpace(): Pair<Int, Int> {
        var totalSpace = 0
        (1..3).forEach {
            if(inventory[it].item is CraftingMemory) totalSpace += (inventory[it].item as CraftingMemory).space
        }
        if(inventory[0].item !is CraftingProcessingUnit) totalSpace = 0
        return Pair(craftingQueue.size, totalSpace)
    }

    var craftingQueue = linkedMapOf<CraftingAction, Int>()
        private set
    var sortedQueue: List<MutableMap.MutableEntry<CraftingAction, Int>>? = null
        private set

    fun cacheSortedQueue(): List<MutableMap.MutableEntry<CraftingAction, Int>> {
        if(sortedQueue == null) {
            sortedQueue = craftingQueue.entries.sortedBy { it.value }.reversed()
        }
        return sortedQueue!!
    }

    fun addCrafting(action: CraftingAction, priority: Int) {
        currentNetwork?.let {
            action.necessaryActions.forEach { act ->
                if (!it.getProcessingActions().containsKey(act))
                    addCrafting(act, priority + 1)
            }
            it.addProcessingAction(action, pos)
        }
        craftingQueue[action] = priority
        sortedQueue = null
        markDirty()
        if(world?.isClient == false) sync()
    }

    fun removeCrafting(action: CraftingAction) {
        currentNetwork?.removeProcessingAction(action)
        craftingQueue.remove(action)
        sortedQueue = null
        markDirty()
        if(world?.isClient == false) sync()
    }

    fun removeCrafting(id: Int) {
        cacheSortedQueue().forEachIndexed { index, (action, _) ->
            if(index == id) {
                currentNetwork?.removeProcessingAction(action)
                craftingQueue.remove(action)
                sortedQueue = null
                markDirty()
                if(world?.isClient == false) sync()
                return
            }
        }
    }

    var delayCount = 0

    override fun tick() {
        super.tick()
        if (world?.isClient == false && craftingQueue.isNotEmpty() && delayCount > 20/getCraftingSpeed()) {
            (0 until getCraftingCores()).forEach {
                val sortedQueue = cacheSortedQueue()
                if(sortedQueue.size >= it+1) {
                    val priority = sortedQueue[it].value
                    val action = sortedQueue[it].key
                    action.state = processCrafting(action, priority)
                    delayCount = 0
                    markDirty()
                    sync()
                }
            }
        }
        delayCount++
    }

    private fun emptyAssembler(be: AssemblerBlockEntity): Boolean {
        return if(be.craftInventory.isEmpty) true
        else {
            (0 until be.craftInventory.size()).forEach {
                currentNetwork!!.insertStack(be.craftInventory.getStack(it))
            }
            return be.craftInventory.isEmpty
        }
    }

    private fun processCrafting(action: CraftingAction, priority: Int): CraftingAction.State {
        //lets check if all the necessary actions are done
        action.necessaryActions.forEach { act ->
            if(act.state != CraftingAction.State.FINISHED)
                return CraftingAction.State.WAITING_SUB
        }
        if(action.quantity == 0) {
            return if(!currentNetwork!!.getProcessingMachines().containsValue(action)) {
                removeCrafting(action)
                CraftingAction.State.FINISHED
            }else CraftingAction.State.FINISHING
        }

        val craftInfo = currentNetwork!!.getCraftableInfo(action.craftStack) ?: return CraftingAction.State.NO_CRAFTABLE
        val machine = world?.getBlockEntity(craftInfo.second) ?: return CraftingAction.State.NO_MACHINE
        val type: CraftingAction.Type = if(craftInfo.first.getString("type") == "crafting") CraftingAction.Type.CRAFTING else CraftingAction.Type.PROCESSING

        if(machine is AssemblerBlockEntity) {
            if(currentNetwork!!.getProcessingMachines().containsKey(machine.pos))
                return CraftingAction.State.MACHINE_BEING_USED
            if(!emptyAssembler(machine))
                return CraftingAction.State.ASSEMBLER_FULL
            currentNetwork!!.addProcessingMachine(machine.pos, action)
            val bl = tryToCraft(action, machine, Direction.DOWN)
            return if(bl) {
                action.quantity--
                CraftingAction.State.PROCESSING
            }else{
                currentNetwork!!.removeProcessingMachine(machine.pos)
                action.state
            }
        }

        if(machine is InterfaceBlockEntity && type == CraftingAction.Type.CRAFTING) {
            var ass: AssemblerBlockEntity? = null
            setOf<BlockPos>(machine.pos.up(), machine.pos.down(), machine.pos.east(), machine.pos.west(), machine.pos.north(), machine.pos.south()).forEach {
                if(!currentNetwork!!.getProcessingMachines().containsKey(it) && world!!.getBlockEntity(it) is AssemblerBlockEntity)
                    ass = world!!.getBlockEntity(it) as AssemblerBlockEntity
            }
            val assembler = ass ?: return CraftingAction.State.NO_ASSEMBLER
            if(!emptyAssembler(assembler))
                return CraftingAction.State.ASSEMBLER_FULL
            var blueprintStack: ItemStack? = null
            var blueprintStackSlot = 0
            machine.blueprintInv.forEachIndexed { idx, stk ->
                if(stk.orCreateTag == craftInfo.first) {
                    blueprintStack = stk
                    blueprintStackSlot = idx
                    return@forEachIndexed
                }
            }
            blueprintStack?.let {
                machine.setStack(blueprintStackSlot, assembler.getStack(0))
                assembler.setStack(0, it)
            }
            val bl = tryToCraft(action, assembler, Direction.DOWN)
            return if(bl) {
                action.quantity--
                CraftingAction.State.PROCESSING
            }else{
                currentNetwork!!.removeProcessingMachine(machine.pos)
                action.state
            }
        }

        if(machine is InterfaceBlockEntity && type == CraftingAction.Type.PROCESSING) {
            if(!machine.cachedState[Interface.DIRECTIONAL])
                return CraftingAction.State.INTERFACE_NOT_DIRECTIONAL

            val targetPos = machine.pos.add(machine.cachedState[Properties.FACING].vector)
            val targetEntity = world!!.getBlockEntity(targetPos) ?: return CraftingAction.State.NO_MACHINE
            val targetState = targetEntity.cachedState
            val targetBlock = targetState.block

            var inventory: Inventory? = null
            if (targetBlock is InventoryProvider) inventory = targetBlock.getInventory(targetState, world, targetPos)
            if (targetEntity is Inventory) {
                inventory = targetEntity
                if (inventory is ChestBlockEntity && targetBlock is ChestBlock) {
                    inventory = ChestBlock.getInventory(targetBlock, targetState, world, targetPos, true)
                }
            }

            inventory ?: return CraftingAction.State.NO_MACHINE_INVENTORY

            if(currentNetwork!!.getProcessingMachines().containsKey(targetPos))
                return CraftingAction.State.MACHINE_BEING_USED

            currentNetwork!!.addProcessingMachine(targetPos, action)
            val outputs = mutableListOf<ItemStack>()
            action.outputStacks.forEach {
                outputs.add(it.copy())
            }
            currentNetwork!!.addProcessingItem(outputs, targetPos)

            val bl = tryToCraft(action, inventory, machine.cachedState[Properties.FACING].opposite)

            return if(bl) {
                action.quantity--
                CraftingAction.State.PROCESSING
            } else {
                currentNetwork!!.removeProcessingMachine(targetPos)
                currentNetwork!!.removeProcessingItem(outputs)
                action.state
            }

        }

        return CraftingAction.State.NOTHING
    }

    enum class ExportAction {
        SUCCESSFUL,
        MISSING_ITEM,
        MACHINE_FULL
    }

    private fun tryToCraft(action: CraftingAction, inventory: Inventory, side: Direction): Boolean {
        var success = true
        if(action.failedStacks.isNotEmpty() || action.missingStacks.isNotEmpty()) {
            val failedIterator = action.failedStacks.iterator()
            while (failedIterator.hasNext()) {
                val failedStack = failedIterator.next()
                val copyStack = failedStack.copy()
                when(tryToExport(copyStack, inventory, side)) {
                    ExportAction.SUCCESSFUL -> failedIterator.remove()
                    ExportAction.MISSING_ITEM -> {
                        action.state = CraftingAction.State.WAITING_ITEM
                        failedIterator.remove()
                        action.missingStacks.add(copyStack.copy())
                    }
                    ExportAction.MACHINE_FULL -> {
                        action.state = if(inventory is AssemblerBlockEntity) CraftingAction.State.ASSEMBLER_FULL else CraftingAction.State.MACHINE_FULL
                        if(!ItemStack.areEqual(failedStack, copyStack)) {
                            failedIterator.remove()
                            action.failedStacks.add(copyStack.copy())
                        }
                    }
                }
            }
            val missingIterator = action.missingStacks.iterator()
            while (missingIterator.hasNext()) {
                val missingStack = missingIterator.next()
                val copyStack = missingStack.copy()
                when(tryToExport(copyStack, inventory, side)) {
                    ExportAction.SUCCESSFUL -> missingIterator.remove()
                    ExportAction.MISSING_ITEM -> {
                        action.state = CraftingAction.State.WAITING_ITEM
                        if(!ItemStack.areEqual(missingStack, copyStack)) {
                            missingIterator.remove()
                            action.missingStacks.add(copyStack.copy())
                        }
                    }
                    ExportAction.MACHINE_FULL -> {
                        action.state = if(inventory is AssemblerBlockEntity) CraftingAction.State.ASSEMBLER_FULL else CraftingAction.State.MACHINE_FULL
                        missingIterator.remove()
                        action.failedStacks.add(copyStack.copy())
                    }
                }
            }
            success = action.failedStacks.isEmpty() && action.missingStacks.isEmpty()
        }else{
            action.inputStacks.forEach {
                val copyStack = it.copy()
                when(tryToExport(copyStack, inventory, side)) {
                    ExportAction.MISSING_ITEM -> {
                        action.state = CraftingAction.State.WAITING_ITEM
                        action.missingStacks.add(copyStack)
                        success = false
                    }
                    ExportAction.MACHINE_FULL -> {
                        action.state = if(inventory is AssemblerBlockEntity) CraftingAction.State.ASSEMBLER_FULL else CraftingAction.State.MACHINE_FULL
                        action.failedStacks.add(copyStack)
                        success = false
                    }
                    else -> {}
                }
            }
        }
        return success
    }

    //rosbei isso do exporter kkk
    private fun tryToExport(stack: ItemStack, inventory: Inventory, side: Direction): ExportAction {
        (if(inventory is SidedInventory) inventory.getAvailableSlots(side).toList() else (0 until inventory.size())).forEach {
            if(inventory !is SidedInventory || inventory.canInsert(it, stack, side)) {
                val stk = inventory.getStack(it)
                if (stk.isEmpty) {
                    val dummyRemoveStack = stack.copy()
                    currentNetwork!!.removeStack(dummyRemoveStack)
                    if(dummyRemoveStack.isEmpty) {
                        inventory.setStack(it, stack.copy())
                        stack.decrement(stack.count)
                        return ExportAction.SUCCESSFUL
                    }else if(!ItemStack.areEqual(dummyRemoveStack, stack)){
                        val inserted = stack.count - dummyRemoveStack.count
                        val dummyStack = stack.copy()
                        dummyStack.count = inserted
                        inventory.setStack(it, dummyStack)
                        stack.decrement(inserted)
                    }
                    return ExportAction.MISSING_ITEM
                } else if(areStacksCompatible(stack, stk)) {
                    if (stk.count+stack.count <= stk.maxCount) {
                        val dummyRemoveStack = stack.copy()
                        currentNetwork!!.removeStack(dummyRemoveStack)
                        if(dummyRemoveStack.isEmpty) {
                            stk.increment(stack.count)
                            stack.decrement(stack.count)
                            return ExportAction.SUCCESSFUL
                        }else if(!ItemStack.areEqual(dummyRemoveStack, stack)){
                            val inserted = stack.count - dummyRemoveStack.count
                            stk.increment(inserted)
                            stack.decrement(inserted)
                        }
                        return ExportAction.MISSING_ITEM
                    }else{
                        val canInsert = stk.maxCount-stk.count
                        val dummyRemoveStack = stack.copy()
                        dummyRemoveStack.count = canInsert
                        currentNetwork!!.removeStack(dummyRemoveStack)
                        if(dummyRemoveStack.isEmpty) {
                            stk.increment(canInsert)
                            stack.decrement(canInsert)
                            return ExportAction.SUCCESSFUL
                        }else if(!ItemStack.areEqual(dummyRemoveStack, stack)){
                            val inserted = canInsert - dummyRemoveStack.count
                            stk.increment(inserted)
                            stack.decrement(inserted)
                            return ExportAction.MISSING_ITEM
                        }
                    }
                }
            }
        }
        return ExportAction.MACHINE_FULL
    }

    override fun size() = inventory.size

    override fun getAvailableSlots(side: Direction?): IntArray {
        return intArrayOf(0, 1, 2, 3)
    }

    override fun isEmpty(): Boolean {
        val iterator = this.inventory.iterator()
        var itemStack: ItemStack
        do {
            if (iterator.hasNext())
                return true
            itemStack = iterator.next()
        } while(itemStack.isEmpty)
        return false
    }

    override fun getStack(slot: Int) = inventory[slot]

    override fun removeStack(slot: Int, amount: Int): ItemStack = Inventories.splitStack(inventory, slot, amount)

    override fun removeStack(slot: Int): ItemStack = Inventories.removeStack(this.inventory, slot)

    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if (stack.count > maxCountPerStack) {
            stack.count = maxCountPerStack
        }
    }

    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?) = true

    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?) = if(slot == 0) (stack?.item is CraftingProcessingUnit) else (stack?.item is CraftingMemory)

    override fun clear()  = inventory.clear()

    override fun canPlayerUse(player: PlayerEntity?): Boolean {
        return if (world!!.getBlockEntity(pos) != this) { false
        } else { player!!.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0 }
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        val craftingQueueTag = ListTag()
        craftingQueue.forEach { (action, priority) ->
            val queueEntryTag = CompoundTag()
            queueEntryTag.put("action", action.toTag(CompoundTag()))
            queueEntryTag.putInt("priority", priority)
            craftingQueueTag.add(queueEntryTag)
        }
        tag.put("craftingQueue", craftingQueueTag)
        Inventories.toTag(tag, inventory)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        super.fromTag(state, tag)
        val it = craftingQueue.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            currentNetwork?.removeProcessingAction(entry.key)
            it.remove()
        }
        sortedQueue = null
        val craftingQueueTag = tag.get("craftingQueue") as ListTag
        craftingQueueTag.forEach {
            val compoundTag = it as CompoundTag
            val action = CraftingAction.fromTag(compoundTag.getCompound("action"), world!!)
            addCrafting(action, compoundTag.getInt("priority"))
        }
        Inventories.fromTag(tag, inventory)
    }
}