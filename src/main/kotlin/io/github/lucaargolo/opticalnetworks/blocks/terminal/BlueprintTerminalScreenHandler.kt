package io.github.lucaargolo.opticalnetworks.blocks.terminal

import io.github.lucaargolo.opticalnetworks.blocks.BLUEPRINT_TERMINAL
import io.github.lucaargolo.opticalnetworks.items.BLUEPRINT
import io.github.lucaargolo.opticalnetworks.items.blueprint.Blueprint
import io.github.lucaargolo.opticalnetworks.mixin.ShapedRecipeMixin
import io.github.lucaargolo.opticalnetworks.mixin.ShapelessRecipeMixin
import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.utils.widgets.GhostSlot
import io.github.lucaargolo.opticalnetworks.network.handlers.NetworkRecipeScreenHandler
import io.github.lucaargolo.opticalnetworks.utils.widgets.TerminalSlot
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.*
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.recipe.*
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.CraftingResultSlot
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World


abstract class BlueprintTerminalScreenHandler(syncId: Int, playerInventory: PlayerInventory, network: Network, entity: BlueprintTerminalBlockEntity, context: ScreenHandlerContext): NetworkRecipeScreenHandler<CraftingInventory>(syncId, playerInventory, network, entity, context), GhostSlot.IScreenHandler, Terminal.IScreenHandler {

    class Crafting(syncId: Int, playerInventory: PlayerInventory, network: Network, entity: BlueprintTerminalBlockEntity, context: ScreenHandlerContext): BlueprintTerminalScreenHandler(syncId, playerInventory, network, entity, context) {

        private var lastRecipe: CraftingRecipe? = null

        init {
            (0..2).forEach { m ->
                (0..2).forEach { l ->
                    ghostSlots.add(GhostSlot(l + m * 3, 25 + l * 18, 0, false))
                }
            }
        }

        override fun addCraftOutSlots() {
            addSlot(CraftingResultSlot(player, craftIn, craftOut, 0, 120, 0))
        }

        override fun isRecipeValid(): Boolean {
            context.run { world, _ ->
                updateResult(syncId, world, player, craftIn, craftOut)
            }
            return !this.slots[0].stack.isEmpty
        }

        override fun getPatternOutSlot() = 38

        override fun getBlueprintTag(): CompoundTag {
            context.run { world, _ ->
                updateResult(syncId, world, player, craftIn, craftOut)
            }
            val tag = CompoundTag()
            tag.putString("type", "crafting")
            tag.putBoolean("useNbt", getBlockEntity().useNbtTag)
            tag.putString("id", lastRecipe!!.id.toString())
            val inputTag = ListTag()
            if(getBlockEntity().useItemTag) {
                if(lastRecipe is ShapelessRecipe) {
                    val input = (lastRecipe as ShapelessRecipeMixin).input
                    for(ingredient in input) {
                        inputTag.add(StringTag.of(ingredient.toJson().toString()))
                    }
                }
                if(lastRecipe is ShapedRecipe) {
                    val inputs = (lastRecipe as ShapedRecipeMixin).inputs
                    for(ingredient in inputs) {
                        inputTag.add(StringTag.of(ingredient.toJson().toString()))
                    }
                }
            }else{
                (0..8).forEach {
                    if(!getGhostInv()[it].isEmpty) inputTag.add(getGhostInv()[it].toTag(CompoundTag()))
                }
            }
            tag.put("input", inputTag)
            tag.put("output", lastRecipe!!.output.toTag(CompoundTag()))
            return tag
        }

        override fun onContentChanged(inventory: Inventory?) {
            super.onContentChanged(inventory)
            context.run { world, _ ->
                updateResult(syncId, world, player, craftIn, craftOut)
            }
        }

        override fun onSlotClick(i: Int, j: Int, actionType: SlotActionType?, playerEntity: PlayerEntity?): ItemStack {
            if(i == 0) return ItemStack.EMPTY
            return super.onSlotClick(i, j, actionType, playerEntity)
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

        override fun getCraftingResultSlotIndex() = 0

    }

    class Processing(syncId: Int, playerInventory: PlayerInventory, network: Network, entity: BlueprintTerminalBlockEntity, context: ScreenHandlerContext): BlueprintTerminalScreenHandler(syncId, playerInventory, network, entity, context) {

        init {
            (0..2).forEach { m ->
                (0..2).forEach { l ->
                    ghostSlots.add(GhostSlot(l + m * 3, 25 + l * 18, 0, true))
                }
            }
        }

        override fun addCraftOutSlots() {
            ghostSlots.add(GhostSlot(9, 115, 0, true))
            ghostSlots.add(GhostSlot(10,115, 0, true))
            ghostSlots.add(GhostSlot(11,115, 0, true))
        }

        override fun isRecipeValid() = !getGhostInv()[9].isEmpty || !getGhostInv()[10].isEmpty || !getGhostInv()[11].isEmpty

        override fun getPatternOutSlot() = 37

        override fun getBlueprintTag(): CompoundTag {
            val tag = CompoundTag()
            tag.putString("type", "processing")
            tag.putBoolean("useNbt", getBlockEntity().useNbtTag)
            val inputTag = ListTag()
            (0..8).forEach {
                if(!getGhostInv()[it].isEmpty) inputTag.add(getGhostInv()[it].toTag(CompoundTag()))
            }
            tag.put("input", inputTag)
            val outputTag = ListTag()
            (9..11).forEach {
                if(!getGhostInv()[it].isEmpty) outputTag.add(getGhostInv()[it].toTag(CompoundTag()))
            }
            tag.put("output", outputTag)
            return tag
        }

        override fun transferSlot(player: PlayerEntity, invSlot: Int): ItemStack? {
            val stack = slots[invSlot].stack
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
        }

        override fun getCraftingResultSlotIndex() = -1

    }

    val player: PlayerEntity = playerInventory.player

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

    val patternOut: Inventory = SimpleInventory(1)

    override val terminalSlots = mutableListOf<TerminalSlot>()
    override val ghostSlots = mutableListOf<GhostSlot>()
    override fun getGhostInv() = getBlockEntity().ghostInv
    override fun getBlockEntity() = entity as BlueprintTerminalBlockEntity

    abstract fun addCraftOutSlots()
    abstract fun isRecipeValid(): Boolean
    abstract fun getPatternOutSlot(): Int
    abstract fun getBlueprintTag(): CompoundTag

    val craftIn: CraftingInventory = object: CraftingInventory(this, 3, 3) {
        override fun size() = 9
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

    val craftOut: CraftingResultInventory = CraftingResultInventory()

    init {
        (0..6).forEach { n ->
            (0..8).forEach { m ->
                terminalSlots.add(TerminalSlot(8 + m * 18, n * 18 + 18))
            }
        }

        this.addCraftOutSlots()

        (0..2).forEach { n ->
            (0..8).forEach { m ->
                addSlot(Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 0))
            }
        }

        (0..8).forEach { n ->
            addSlot(Slot(playerInventory, n,  8 + n * 18, 0))
        }

        this.addSlot(object: Slot(patternIn, 0, 8 + 8 * 18, 0) {
            override fun canInsert(stack: ItemStack?) = stack?.item is Blueprint
        })

        this.addSlot(object: Slot(patternOut, 0, 8 + 8 * 18, 0) {
            override fun canInsert(itemStack_1: ItemStack?) = false
            override fun onTakeItem(player: PlayerEntity?, stack: ItemStack?): ItemStack {
                patternIn.getStack(0).decrement(1)
                patternIn.markDirty()
                onContentChanged(patternIn)
                return super.onTakeItem(player, stack)
            }
        })

        this.onContentChanged(craftIn)
    }

    override fun onContentChanged(inventory: Inventory?) {
        super.onContentChanged(inventory)
        context.run { world, _ ->
            updateBlueprintResult(syncId, world, player, patternIn, patternOut)
        }
    }

    private fun updateBlueprintResult(syncId: Int, world: World, player: PlayerEntity, patternIn: Inventory, patternOut: Inventory) {
        if (!world.isClient) {
            val serverPlayerEntity = player as ServerPlayerEntity
            var itemStack = ItemStack.EMPTY
            if (patternIn.getStack(0).item is Blueprint && !patternIn.getStack(0).isEmpty && isRecipeValid()) {
                itemStack = ItemStack(BLUEPRINT)
                itemStack.tag = getBlueprintTag()
            }
            patternOut.setStack(0, itemStack)
            serverPlayerEntity.networkHandler.sendPacket(ScreenHandlerSlotUpdateS2CPacket(syncId, getPatternOutSlot(), itemStack))
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