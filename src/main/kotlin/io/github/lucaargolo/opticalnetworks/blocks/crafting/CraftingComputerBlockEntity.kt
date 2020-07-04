package io.github.lucaargolo.opticalnetworks.blocks.crafting

import io.github.lucaargolo.opticalnetworks.blocks.`interface`.Interface
import io.github.lucaargolo.opticalnetworks.blocks.`interface`.InterfaceBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.assembler.AssemblerBlockEntity
import io.github.lucaargolo.opticalnetworks.items.basic.CraftingDisc
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
import net.minecraft.state.property.Properties
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class CraftingComputerBlockEntity(block: Block): NetworkBlockEntity(block), SidedInventory {

    var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(4, ItemStack.EMPTY)

    private var craftingQueue = linkedMapOf<CraftingAction, Int>()

    fun addCrafting(action: CraftingAction, priority: Int) {
        action.state = CraftingAction.State.PROCESSING
        currentNetwork?.processingActions?.put(action, pos)
        craftingQueue[action] = priority
        markDirty()
        sync()
    }

    fun removeCrafting(action: CraftingAction) {
        currentNetwork?.processingActions?.remove(action)
        craftingQueue.remove(action)
        markDirty()
        sync()
    }

    fun removeCrafting(id: Int) {
        val it = craftingQueue.iterator()
        var index = 0
        while (it.hasNext()) {
            val entry = it.next()
            if(index == id) {
                currentNetwork?.processingActions?.remove(entry.key)
                it.remove()
                break;
            }
            index++
        }
        markDirty()
        sync()
    }

    var delayCount = 0

    override fun tick() {
        super.tick()
        if (world?.isClient == false && craftingQueue.isNotEmpty()) {
            val sortedQueue = craftingQueue.entries.sortedBy { it.value }.reversed()
            val priority = sortedQueue[0].value
            val action = sortedQueue[0].key
            //opa vamos ver se precisa de algum subcrafting, se sim vamos ver se ele ta adicionado na network, se não vamos adicionar nesse computador (mas isso ainda falta alguns checks)
            if(action.necessaryActions.isNotEmpty()) {
                action.necessaryActions.forEach {
                    if(it.state != CraftingAction.State.FINISHED) {
                        //aqui eu ainda tenho q checar se tem cpu e colocar um State.MISSING_CPU
                        if(currentNetwork!!.processingActions[it] == null)
                            addCrafting(it, priority+1)
                        action.state = CraftingAction.State.WAITING_SUB
                        return@forEach
                    }
                    action.state = CraftingAction.State.PROCESSING
                }
            }
            //aqui vamos começar a processar o crafting
            if(action.state != CraftingAction.State.WAITING_SUB) {
                if(action.quantity > 0) {
                    val machine = world!!.getBlockEntity(action.machinePos)
                    if(machine is AssemblerBlockEntity) {
                        if (!currentNetwork!!.processingMachines.contains(action.machinePos)) {
                            action.state = CraftingAction.State.PROCESSING
                            currentNetwork!!.processingMachines.add(action.machinePos!!)
                            if(tryToCraft(action, machine, Direction.DOWN)) action.quantity--
                            else currentNetwork!!.processingMachines.remove(action.machinePos!!)
                        } else action.state = CraftingAction.State.WAITING_MACHINE
                    }
                    if(machine is InterfaceBlockEntity) {
                        if(action.type == CraftingAction.Type.CRAFTING) {
                            //e vamos procurar algum assembler vazio
                            val machinePos = action.machinePos!!
                            var be: AssemblerBlockEntity? = null
                            setOf<BlockPos>(machinePos.up(), machinePos.down(), machinePos.east(), machinePos.west(), machinePos.north(), machinePos.south()).forEach {
                                if(!currentNetwork!!.processingMachines.contains(it) && world!!.getBlockEntity(it) is AssemblerBlockEntity)
                                    be = world!!.getBlockEntity(it) as AssemblerBlockEntity
                            }
                            if(be != null) {
                                action.state = CraftingAction.State.PROCESSING
                                currentNetwork!!.processingMachines.add(be!!.pos)
                                //e vamos trocar a blueprint de uma pra outra
                                var blueprintStack: ItemStack? = null
                                var blueprintStackSlot = 0
                                machine.blueprintInv.forEachIndexed { idx, stk ->
                                    if(stk.orCreateTag == action.tag) {
                                        blueprintStack = stk
                                        blueprintStackSlot = idx
                                        return@forEachIndexed
                                    }
                                }
                                blueprintStack?.let {
                                    machine.setStack(blueprintStackSlot, be!!.getStack(0))
                                    be!!.setStack(0, it)
                                }
                                if(tryToCraft(action, be!!, Direction.DOWN)) action.quantity--
                                else currentNetwork!!.processingMachines.remove(be!!.pos)
                            }else{
                                action.state = CraftingAction.State.WAITING_MACHINE
                            }
                        }
                        if(action.type == CraftingAction.Type.PROCESSING) {
                            val interfaceState = world!!.getBlockState(machine.pos)

                            val targetPos = machine.pos.add(interfaceState[Properties.FACING].vector)
                            val targetState = world!!.getBlockState(targetPos)
                            val targetEntity = world!!.getBlockEntity(targetPos)
                            val targetBlock = targetState.block
                            var inventory: Inventory? = null
                            if (targetBlock is InventoryProvider) inventory = targetBlock.getInventory(targetState, world, targetPos)
                            if (targetEntity is Inventory) {
                                inventory = targetEntity
                                if (inventory is ChestBlockEntity && targetBlock is ChestBlock) {
                                    inventory = ChestBlock.getInventory(targetBlock, targetState, world, targetPos, true)
                                }
                            }

                            if(interfaceState[Interface.DIRECTIONAL] && inventory != null && !currentNetwork!!.processingMachines.contains(targetPos)) {
                                action.state = CraftingAction.State.PROCESSING
                                currentNetwork!!.processingMachines.add(targetPos)
                                val outputs = mutableListOf<ItemStack>()
                                action.outputStacks.forEach {
                                    outputs.add(it.copy())
                                }
                                currentNetwork!!.processingItems[outputs] = targetPos
                                if(tryToCraft(action, inventory, interfaceState[Properties.FACING].opposite)) action.quantity--
                                else {
                                    currentNetwork!!.processingMachines.remove(targetPos)
                                    currentNetwork!!.processingItems.remove(outputs)
                                }
                            } else action.state = CraftingAction.State.WAITING_MACHINE
                        }
                    }
                }else{
                    action.state = CraftingAction.State.FINISHED
                    removeCrafting(action)
                }
            }
            delayCount = 0
        }
        delayCount++
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
                        action.state = CraftingAction.State.WAITING_MACHINE
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
                        action.state = CraftingAction.State.WAITING_MACHINE
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
                        action.state = CraftingAction.State.WAITING_MACHINE
                        action.failedStacks.add(copyStack)
                        success = false
                    }
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
                        return ExportAction.MISSING_ITEM
                    }
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
                            return ExportAction.MISSING_ITEM
                        }
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

    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?) = if(slot == 0) (stack?.item is CraftingProcessingUnit) else (stack?.item is CraftingDisc)

    override fun clear()  = inventory.clear()

    override fun canPlayerUse(player: PlayerEntity?): Boolean {
        return if (world!!.getBlockEntity(pos) != this) { false
        } else { player!!.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0 }
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        Inventories.toTag(tag, inventory)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        super.fromTag(state, tag)
        Inventories.fromTag(tag, inventory)
    }
}