package io.github.lucaargolo.opticalnetworks.blocks.terminal

import io.github.lucaargolo.opticalnetworks.blocks.BLUEPRINT_TERMINAL
import io.github.lucaargolo.opticalnetworks.items.basic.Blueprint
import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.network.SYNCHRONIZE_LAST_RECIPE_PACKET
import io.github.lucaargolo.opticalnetworks.utils.GhostSlot
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

class BlueprintTerminalScreenHandler(syncId: Int, playerInventory: PlayerInventory, network: Network, entity: BlueprintTerminalBlockEntity, context: ScreenHandlerContext): NetworkRecipeScreenHandler<CraftingInventory>(syncId, playerInventory, network, entity, context), GhostSlot.GhostSlotScreenHandler, TerminalScreenHandlerInterface {

    private val player: PlayerEntity = playerInventory.player

    private val craftIn: CraftingInventory = object: CraftingInventory(this, 3, 3) {
        override fun size() = getBlockEntity().ghostInv.size
        override fun isEmpty() = getBlockEntity().ghostInv.isEmpty()
        override fun getStack(slot: Int) = getBlockEntity().ghostInv[slot]
        override fun canPlayerUse(player: PlayerEntity?) = getBlockEntity().canPlayerUse(player)
        override fun clear() = getBlockEntity().ghostInv.clear()

        override fun removeStack(slot: Int): ItemStack? {
            val stack = Inventories.removeStack(getBlockEntity().ghostInv, slot)
            onContentChanged(this)
            return stack
        }
        override fun removeStack(slot: Int, amount: Int): ItemStack? {
            val stack = Inventories.splitStack(getBlockEntity().ghostInv, slot, amount)
            onContentChanged(this)
            return stack
        }
        override fun setStack(slot: Int, stack: ItemStack) {
            getBlockEntity().ghostInv[slot] = stack
            if(stack.count > maxCountPerStack) {
                stack.count = maxCountPerStack
            }
            onContentChanged(this)
        }
        override fun markDirty() {
            getBlockEntity().markDirty()
        }

    }
    private val patternIn: Inventory = object: Inventory {
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
    private val craftOut: CraftingResultInventory = CraftingResultInventory()
    private val patternOut: CraftingResultInventory = CraftingResultInventory()

    override val terminalSlots = mutableListOf<TerminalSlot>()
    override val ghostSlots = mutableListOf<GhostSlot>()
    override fun getGhostInv() = getBlockEntity().ghostInv
    override fun getBlockEntity() = entity as BlueprintTerminalBlockEntity

    init {
        (0..6).forEach { n ->
            (0..8).forEach { m ->
                terminalSlots.add(TerminalSlot(8 + m * 18, n * 18 + 18))
            }
        }

        addSlot(CraftingResultSlot(player, craftIn, craftOut, 0, 120, 0))

        (0..2).forEach { m ->
            (0..2).forEach { l ->
                ghostSlots.add(GhostSlot(l + m * 3,25 + l * 18, 0))
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

        addSlot(object: Slot(patternIn, 0, 8 + 8 * 18, 0) {
            override fun canInsert(stack: ItemStack?) = stack?.item is Blueprint
        })

        addSlot(object: Slot(patternOut, 0, 8 + 8 * 18, 0) {
            override fun canInsert(itemStack_1: ItemStack?) = false
        })

        onContentChanged(craftIn)
    }

    override fun onSlotClick(i: Int, j: Int, actionType: SlotActionType?, playerEntity: PlayerEntity?): ItemStack {
        if(i == 0) return ItemStack.EMPTY
        return super.onSlotClick(i, j, actionType, playerEntity)
    }

    override fun transferSlot(player: PlayerEntity, index: Int): ItemStack? {
        var itemStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot != null && slot.hasStack()) {
            val itemStack2 = slot.stack
            itemStack = itemStack2.copy()
            if (index == 0) {
                return ItemStack.EMPTY
            } else if (index in (1..36)) {
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
            } else if (!insertItem(itemStack2, 1, 37, false)) {
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

    private fun updateResult(syncId: Int, world: World, player: PlayerEntity, craftingInventory: CraftingInventory, resultInventory: CraftingResultInventory) {
        if (!world.isClient) {
            val serverPlayerEntity = player as ServerPlayerEntity
            var itemStack = ItemStack.EMPTY
            val optional = world.server!!.recipeManager.getFirstMatch(RecipeType.CRAFTING, craftingInventory, world)
            if (optional.isPresent) {
                val craftingRecipe = optional.get()
                if (resultInventory.shouldCraftRecipe(world, serverPlayerEntity, craftingRecipe)) {
                    itemStack = craftingRecipe.craft(craftingInventory)
                }
            }
            resultInventory.setStack(0, itemStack)
            serverPlayerEntity.networkHandler.sendPacket(ScreenHandlerSlotUpdateS2CPacket(syncId, 0, itemStack))
        }
    }

    override fun onContentChanged(inventory: Inventory?) {
        context.run { world, _ ->
            updateResult(syncId, world, player, craftIn, craftOut)
        }
    }

    override fun populateRecipeFinder(finder: RecipeFinder?) {
        craftIn.provideRecipeInputs(finder)
    }

    override fun clearCraftingSlots() {
        getBlockEntity().ghostInv.clear()
    }

    override fun matches(recipe: Recipe<in CraftingInventory?>): Boolean {
        return recipe.matches(craftIn, player.world)
    }

    override fun getCraftingResultSlotIndex() = -1

    override fun getCraftingWidth(): Int {
        return craftIn.width
    }

    override fun getCraftingHeight(): Int {
        return craftIn.height
    }

    @Environment(EnvType.CLIENT)
    override fun getCraftingSlotCount() = 0

    override fun canUse(player: PlayerEntity): Boolean {
        return context.run({ world: World, blockPos: BlockPos ->
            if (world.getBlockState(
                    blockPos
                ).block != BLUEPRINT_TERMINAL
            ) false else player.squaredDistanceTo(
                blockPos.x + .5,
                blockPos.y + .5,
                blockPos.z + .5
            ) < 64.0
        }, true)
    }

}