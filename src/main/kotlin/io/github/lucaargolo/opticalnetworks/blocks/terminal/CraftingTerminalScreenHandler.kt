package io.github.lucaargolo.opticalnetworks.blocks.terminal

import io.github.lucaargolo.opticalnetworks.blocks.CRAFTING_TERMINAL
import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.network.SYNCHRONIZE_LAST_RECIPE_PACKET
import io.github.lucaargolo.opticalnetworks.network.areStacksCompatible
import io.github.lucaargolo.opticalnetworks.utils.NetworkRecipeScreenHandler
import io.netty.buffer.Unpooled
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.CraftingInventory
import net.minecraft.inventory.CraftingResultInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.recipe.*
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.CraftingResultSlot
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class CraftingTerminalScreenHandler(syncId: Int, playerInventory: PlayerInventory, network: Network, entity: CraftingTerminalBlockEntity, context: ScreenHandlerContext): NetworkRecipeScreenHandler<CraftingInventory>(syncId, playerInventory, network, entity, context), TerminalScreenHandlerInterface {

    private val player: PlayerEntity = playerInventory.player

    @Suppress("DuplicatedCode")
    private val input: CraftingInventory = object: CraftingInventory(this, 3, 3) {
        override fun size() = getBlockEntity().size()
        override fun isEmpty() = getBlockEntity().isEmpty
        override fun getStack(slot: Int) = getBlockEntity().getStack(slot)
        override fun canPlayerUse(player: PlayerEntity?) = getBlockEntity().canPlayerUse(player)
        override fun clear() = getBlockEntity().clear()

        override fun removeStack(slot: Int): ItemStack? {
            val stack: ItemStack = getBlockEntity().removeStack(slot)
            onContentChanged(this)
            return stack
        }
        override fun removeStack(slot: Int, amount: Int): ItemStack? {
            val stack: ItemStack = getBlockEntity().removeStack(slot, amount)
            onContentChanged(this)
            return stack
        }
        override fun setStack(slot: Int, stack: ItemStack?) {
            getBlockEntity().setStack(slot, stack)
            onContentChanged(this)
        }
        override fun markDirty() {
            getBlockEntity().markDirty()
        }
    }

    private val result: CraftingResultInventory = CraftingResultInventory()

    var lastRecipe: CraftingRecipe? = null
    override val terminalSlots = mutableListOf<TerminalSlot>()

    init {
        (0..6).forEach { n ->
            (0..8).forEach { m ->
                terminalSlots.add(TerminalSlot(8 + m * 18, n * 18 + 18))
            }
        }

        addSlot(CraftingResultSlot(player, input, result, 0, 120, 0))

        (0..2).forEach { m ->
           (0..2).forEach { l ->
               addSlot(Slot(input, l + m * 3, 26 + l * 18, 0))
           }
        }

        (0..2).forEach { n ->
            (0..8).forEach { m ->
                addSlot(Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 0))
            }
        }

        (0..8).forEach { n ->
            addSlot(Slot(playerInventory, n,  8 + n * 18, 0))
        }

        onContentChanged(input)
    }

    fun getBlockEntity() = entity as CraftingTerminalBlockEntity

    override fun onSlotClick(slotId: Int, clickData: Int, actionType: SlotActionType, playerEntity: PlayerEntity): ItemStack {
        val quickMoveCache = mutableListOf<ItemStack>()
        if(slotId == 0 && slots[0].hasStack()) {
            if(actionType == SlotActionType.PICKUP && (playerEntity.inventory.cursorStack.isEmpty || (areStacksCompatible(playerEntity.inventory.cursorStack, slots[0].stack) && playerEntity.inventory.cursorStack.count+slots[0].stack.count <= slots[0].stack.maxCount))) {
                (1..10).forEach {
                    if(!slots[it].stack.isEmpty && slots[it].stack.count == 1) {
                        val dummyStack = slots[it].stack.copy()
                        if(network.removeStack(dummyStack)) {
                            slots[it].stack.increment(1)
                        }
                    }
                }
            }
            if(actionType == SlotActionType.QUICK_MOVE) {
                val toCraft = lastRecipe!!.output.maxCount/lastRecipe!!.output.count
                (1..10).forEach {
                    val dummyStack = slots[it].stack.copy()
                    dummyStack.count = 1
                    quickMoveCache.add(dummyStack)
                }
                (0 until toCraft).forEach { _ ->
                    (1..10).forEach {
                        if(!slots[it].stack.isEmpty && slots[it].stack.count < toCraft) {
                            val dummyStack = slots[it].stack.copy()
                            dummyStack.count = 1
                            if(network.removeStack(dummyStack)) slots[it].stack.increment(1)
                        }
                    }
                }
            }
        }
        val result = super.onSlotClick(slotId, clickData, actionType, playerEntity)
        if(quickMoveCache.isNotEmpty()) {
            quickMoveCache.forEachIndexed { idx, stk ->
                if(network.removeStack(stk.copy())) slots[idx+1].stack = stk
            }
        }
        return result
    }

    private fun updateResult(syncId: Int, world: World, player: PlayerEntity, craftingInventory: CraftingInventory, resultInventory: CraftingResultInventory) {
        if (!world.isClient) {
            val serverPlayerEntity = player as ServerPlayerEntity
            var itemStack = ItemStack.EMPTY
            val optional = world.server!!.recipeManager.getFirstMatch(RecipeType.CRAFTING, craftingInventory, world)
            if (optional.isPresent) {
                val craftingRecipe = optional.get()
                if (resultInventory.shouldCraftRecipe(world, serverPlayerEntity, craftingRecipe)) {
                    itemStack = craftingRecipe.craft(craftingInventory)
                    lastRecipe = craftingRecipe
                    val passedData = PacketByteBuf(Unpooled.buffer())
                    passedData.writeInt(if(lastRecipe is ShapedRecipe) 0 else 1)
                    passedData.writeIdentifier(craftingRecipe.id)
                    if(craftingRecipe is ShapedRecipe)
                        RecipeSerializer.SHAPED.write(passedData, craftingRecipe)
                    if(craftingRecipe is ShapelessRecipe)
                        RecipeSerializer.SHAPELESS.write(passedData, craftingRecipe)
                    ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, SYNCHRONIZE_LAST_RECIPE_PACKET, passedData)
                }
            }
            resultInventory.setStack(0, itemStack)
            serverPlayerEntity.networkHandler.sendPacket(ScreenHandlerSlotUpdateS2CPacket(syncId, 0, itemStack))
        }
    }


    override fun transferSlot(player: PlayerEntity, index: Int): ItemStack? {
        var itemStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot != null && slot.hasStack()) {
            val itemStack2 = slot.stack
            itemStack = itemStack2.copy()
            if (index == 0) {
                context.run { world: World?, blockPos: BlockPos? ->
                    itemStack2.item.onCraft(itemStack2, world, player)
                }
                if (!insertItem(itemStack2, 10, 46, true)) {
                    return ItemStack.EMPTY
                }
                slot.onStackChanged(itemStack2, itemStack)
            } else if (index in (10..45)) {
                val stack = slots[index].stack
                return if(!stack.isEmpty) {
                    val backupStack = stack.copy()
                    val space = network.getSpace()
                    val availableSpace = space.second-space.first
                    if(stack.count > availableSpace) {
                        stack.decrement(availableSpace)
                        backupStack.count = availableSpace
                        if(!player.world.isClient) network.insertStack(backupStack)
                    }else{
                        stack.decrement(stack.count)
                        if(!player.world.isClient) network.insertStack(backupStack)
                    }
                    backupStack
                }else ItemStack.EMPTY
            } else if (!insertItem(itemStack2, 10, 46, false)) {
                return ItemStack.EMPTY
            }
            if (itemStack2.isEmpty) {
                slot.stack = ItemStack.EMPTY
            } else {
                slot.markDirty()
            }
            if (itemStack2.count == itemStack.count) {
                return ItemStack.EMPTY
            }
            val itemStack3 = slot.onTakeItem(player, itemStack2)
            if (index == 0) {
                player.dropItem(itemStack3, false)
            }
        }
        return itemStack
    }

    override fun onContentChanged(inventory: Inventory?) {
        context.run { world, _ ->
            updateResult(syncId, world, player, input, result)
        }
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return context.run({ world: World, blockPos: BlockPos ->
            if (world.getBlockState(
                    blockPos
                ).block != CRAFTING_TERMINAL
            ) false else player.squaredDistanceTo(
                blockPos.x + .5,
                blockPos.y + .5,
                blockPos.z + .5
            ) < 64.0
        }, true)
    }

    override fun populateRecipeFinder(finder: RecipeFinder?) {
        input.provideRecipeInputs(finder)
    }

    override fun clearCraftingSlots() {
        input.clear()
        result.clear()
    }

    override fun matches(recipe: Recipe<in CraftingInventory?>): Boolean {
        return recipe.matches(input, player.world)
    }

    override fun getCraftingResultSlotIndex(): Int {
        return 0
    }

    override fun getCraftingWidth(): Int {
        return input.width
    }

    override fun getCraftingHeight(): Int {
        return input.height
    }

    @Environment(EnvType.CLIENT)
    override fun getCraftingSlotCount(): Int {
        return 10
    }

}