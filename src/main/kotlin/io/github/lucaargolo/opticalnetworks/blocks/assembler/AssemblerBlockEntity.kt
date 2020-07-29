package io.github.lucaargolo.opticalnetworks.blocks.assembler

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.lucaargolo.opticalnetworks.items.blueprint.Blueprint
import io.github.lucaargolo.opticalnetworks.mixin.ShapedRecipeMixin
import io.github.lucaargolo.opticalnetworks.mixin.ShapelessRecipeMixin
import io.github.lucaargolo.opticalnetworks.utils.areStacksCompatible
import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.CraftingInventory
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.recipe.*
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.math.Direction
import net.minecraft.util.registry.Registry

class AssemblerBlockEntity(block: Block): NetworkBlockEntity(block), SidedInventory {

    var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(11, ItemStack.EMPTY)

    val craftInventory: CraftingInventory = object: CraftingInventory(null, 3, 3) {
        override fun size() = 9
        override fun isEmpty() = inventory.subList(1, 9).isEmpty()
        override fun getStack(slot: Int) = inventory[slot+1]
        override fun canPlayerUse(player: PlayerEntity?) = this@AssemblerBlockEntity.canPlayerUse(player)
        override fun clear() = inventory.clear()

        override fun removeStack(slot: Int): ItemStack? {
            return Inventories.removeStack(inventory, slot+1)
        }
        override fun removeStack(slot: Int, amount: Int): ItemStack? {
            return Inventories.splitStack(inventory, slot+1, amount)
        }
        override fun setStack(slot: Int, stack: ItemStack) {
            inventory[slot+1] = stack
            if(stack.count > maxCountPerStack) {
                stack.count = maxCountPerStack
            }
        }
        override fun markDirty() {
            this@AssemblerBlockEntity.markDirty()
        }

        override fun provideRecipeInputs(finder: RecipeFinder) {
            val it = inventory.subList(1, 9).iterator()

            while (it.hasNext()) {
                val itemStack = it.next() as ItemStack
                finder.addNormalItem(itemStack)
            }
        }

    }

    var lastInputSlot = 0

    companion object {
        const val PROCESSING_GOAL = 0
    }

    var processingTime = 0
    var processing = false

    private fun getStringStacks(ingredientJson: String): MutableList<ItemStack> {
        val inputList = mutableListOf<ItemStack>()
        val inputJson: JsonObject? = try {
            JsonParser().parse(ingredientJson).asJsonObject
        } catch (ignored: Exception) {
            null
        }
        inputJson?.let { json ->
            val mcItem: String? = try {
                json.get("item").asString
            } catch (ignored: Exception) {
                null
            }
            val mcTag: String? = try {
                json.get("tag").asString
            } catch (ignored: Exception) {
                null
            }
            mcItem?.let { str ->
                val id = Identifier(str)
                val item = Registry.ITEM.get(id)
                val stk = ItemStack(item)
                if (!stk.isEmpty) inputList.add(stk)
            }
            mcTag?.let { str ->
                world?.server?.tagManager?.items()?.get(Identifier(str))?.values()?.forEach { item ->
                    val stk = ItemStack(item)
                    if (!stk.isEmpty) inputList.add(stk)
                }
            }
        }
        return inputList
    }

    private fun getInputList(): MutableList<ItemStack> {
        val stack = inventory[0]
        val inputList = mutableListOf<ItemStack>()
        val listTag = stack.tag!!.get("input") as ListTag
        listTag.forEach {tag ->
            if (tag is StringTag) {
                val inputString = tag.asString()
                inputList.addAll(getStringStacks(inputString))
            }
            if (tag is CompoundTag) {
                val stk = ItemStack.fromTag(tag)
                inputList.add(stk)
            }
        }
        return inputList
    }

    private fun isOutputValid(): Boolean {
        val stack = inventory[10]
        val output = ItemStack.fromTag(inventory[0].tag!!.getCompound("output"))
        return (stack.isEmpty) || (areStacksCompatible(
            output,
            stack
        ) && stack.count < stack.maxCount)
    }

    private fun isInputValid(): Boolean {
        val stack = inventory[0]
        val useNbt = stack.tag!!.getBoolean("useNbt")
        val inputList = getInputList()
        //This loops through all the stored items and checks if the recipe items match
        inventory.subList(1,9).forEach {storedStk ->
            var aux = true
            if(!storedStk.isEmpty) {
                val it = inputList.iterator()
                while(it.hasNext()) {
                    val inputStk = it.next()
                    aux = if(useNbt) areStacksCompatible(storedStk, inputStk)
                    else storedStk.isItemEqualIgnoreDamage(inputStk)
                    if(aux) {
                        it.remove()
                        break
                    }
                }
            }
            if(!aux) return false
        }
        return true
    }

    fun isBlueprintValid(): Boolean {
        val blueprintStack = inventory[0]
        return blueprintStack.item is Blueprint
                && blueprintStack.hasTag()
                && blueprintStack.tag!!.contains("type")
                && blueprintStack.tag!!.getString("type") == "crafting"
                && blueprintStack.tag!!.contains("id")
                && blueprintStack.tag!!.contains("input")
                && blueprintStack.tag!!.contains("output")
                && blueprintStack.tag!!.contains("useNbt")

    }

    override fun tick() {
        super.tick()
        if(world?.isClient == false) {
            processing = isBlueprintValid() && isOutputValid() && isInputValid()
            if(processing) {
                val id = Identifier(inventory[0].tag!!.getString("id"))
                val tagRecipeOptional = world!!.recipeManager.get(id)
                val tagRecipe: CraftingRecipe? = if(tagRecipeOptional.isPresent && tagRecipeOptional.get() is CraftingRecipe) tagRecipeOptional.get() as CraftingRecipe else null
                val invRecipeOptional = world!!.server!!.recipeManager.getFirstMatch(RecipeType.CRAFTING, craftInventory, world)
                val invRecipe: CraftingRecipe? = if (invRecipeOptional.isPresent) invRecipeOptional.get() else null
                if(tagRecipe != null && tagRecipe == invRecipe) {
                    if(processingTime >= PROCESSING_GOAL) {
                        inventory.subList(1, 9).forEach { it.decrement(1) }
                        val stk = inventory[10]
                        if (stk.isEmpty) inventory[10] = tagRecipe.output.copy()
                        else if (areStacksCompatible(stk, tagRecipe.output) && stk.count+tagRecipe.output.count <= stk.maxCount) inventory[10].increment(tagRecipe.output.count)
                        inventory[10] = currentNetwork!!.insertStack(inventory[10])
                        currentNetwork!!.removeProcessingMachine(pos)
                        lastInputSlot = 0
                        processingTime = 0
                    }else processingTime++
                }else processingTime = 0
            }else processingTime = 0
            markDirty()
            sync()
        }
    }

    override fun size() = inventory.size

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

    override fun clear()  = inventory.clear()

    override fun canPlayerUse(player: PlayerEntity?): Boolean {
        return if (world!!.getBlockEntity(pos) != this) {
            false
        } else {
            player!!.squaredDistanceTo(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        tag.putInt("processingTime", processingTime)
        tag.putBoolean("processing", processing)
        Inventories.toTag(tag, inventory)
        return super.toTag(tag)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        processingTime = tag.getInt("processingTime")
        processing = tag.getBoolean("processing")
        super.fromTag(state, tag)
        Inventories.fromTag(tag, inventory)
    }

    override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?) = (slot == 10)

    override fun canInsert(slot: Int, stack: ItemStack, dir: Direction?): Boolean {
        if (slot == 0 && stack.item is Blueprint && stack.hasTag() && stack.tag!!.contains("type") && stack.tag!!.getString("type") == "crafting")
            return true
        if (slot in (1..9) && isBlueprintValid()) {
            val id = Identifier(inventory[0].tag!!.getString("id"))
            val tagRecipeOptional = world!!.recipeManager.get(id)
            val tagRecipe: CraftingRecipe? = if(tagRecipeOptional.isPresent && tagRecipeOptional.get() is CraftingRecipe) tagRecipeOptional.get() as CraftingRecipe else null
            tagRecipe?.let { recipe ->
                var ingredients: DefaultedList<Ingredient>? = null
                var width = 3
                var height = 3
                if (recipe is ShapedRecipe) {
                    ingredients = (recipe as ShapedRecipeMixin).inputs
                    width = recipe.width
                    height = recipe.height
                }
                if (recipe is ShapelessRecipe)
                    ingredients = (recipe as ShapelessRecipeMixin).input
                ingredients?.let {
                    val validSlots = linkedMapOf<Int, List<ItemStack>>()
                    ingredients.forEachIndexed { idx, igd ->
                        val validStacks = getStringStacks(igd.toJson().toString())
                        if(validStacks.isNotEmpty()) {
                            if(width < 3 && height < 3) {
                                val row = (idx+1)/height
                                val cu = idx+1+(row*(3-width))
                                validSlots[cu] = validStacks
                            }else{
                                validSlots[idx+1] = validStacks
                            }
                        }
                    }
                    if(validSlots[slot] != null) {
                        val validStacks = validSlots[slot]!!
                        validStacks.forEach { stk ->
                            if(areStacksCompatible(stk, stack)) {
                                lastInputSlot = slot
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    override fun getAvailableSlots(side: Direction?): IntArray {
        if(!isBlueprintValid()) return intArrayOf(0, 10)
        val id = Identifier(inventory[0].tag!!.getString("id"))
        val tagRecipeOptional = world!!.recipeManager.get(id)
        val tagRecipe: CraftingRecipe? = if(tagRecipeOptional.isPresent && tagRecipeOptional.get() is CraftingRecipe) tagRecipeOptional.get() as CraftingRecipe else null
        var validSlots = mutableListOf<Int>()
        tagRecipe?.let { recipe ->
            var ingredients: DefaultedList<Ingredient>? = null
            var width = 3
            var height = 3
            if(recipe is ShapedRecipe) {
                ingredients = (recipe as ShapedRecipeMixin).inputs
                width = recipe.width
                height = recipe.height
            }
            if(recipe is ShapelessRecipe)
                ingredients = (recipe as ShapelessRecipeMixin).input
            ingredients?.let {
                ingredients.forEachIndexed { idx, igd ->
                    val validStacks = getStringStacks(igd.toJson().toString())
                    if(validStacks.isNotEmpty()) {
                        if(width < 3 && height < 3) {
                            val row = (idx+1)/height
                            val cu = idx+1+(row*(3-width))
                            validSlots.add(cu)
                        }else{
                            validSlots.add(idx+1)
                        }
                    }
                }
            }
        }

        var find = validSlots.indexOf(lastInputSlot)
        if(find+1 > validSlots.size) find = 0
        val nl = mutableListOf<Int>()
        nl.addAll(validSlots.subList(find+1, validSlots.size))
        nl.addAll(validSlots.subList(0, find+1))
        validSlots = nl

        val finalSlots = mutableListOf(0, 10)
        finalSlots.addAll(validSlots)
        return finalSlots.toIntArray()
    }

}