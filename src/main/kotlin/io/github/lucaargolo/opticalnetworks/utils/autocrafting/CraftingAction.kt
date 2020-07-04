package io.github.lucaargolo.opticalnetworks.utils.autocrafting

import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.utils.areStacksCompatible
import io.github.lucaargolo.opticalnetworks.utils.getStackFromTag
import io.github.lucaargolo.opticalnetworks.utils.getTagFromStack
import io.github.lucaargolo.opticalnetworks.utils.IngredientHelper
import io.github.lucaargolo.opticalnetworks.utils.TagStack
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper

class CraftingAction(val network: Network?, val stacks: MutableList<ItemStack>, val craftables: MutableList<ItemStack>, val world: ServerWorld?, val craftStack: ItemStack, var requestedQuantity: Int) {

    enum class Type {
        CRAFTING,
        PROCESSING
    }

    enum class State {
        NOTHING,
        PROCESSING,
        WAITING_SUB,
        WAITING_CPU,
        WAITING_ITEM,
        WAITING_MACHINE,
        FINISHED
    }

    val necessaryActions: MutableList<CraftingAction> = mutableListOf()
    //tags that are missing for the crafting (used in request screen only)
    val missingTags: MutableList<TagStack> = mutableListOf()
    //stacks that are missing for the crafting (used in request screen and in crafting computer in case someone removed the item after the craft started)
    val missingStacks: MutableList<ItemStack> = mutableListOf()
    //stacks that are available in the system
    val availableStacks: MutableList<ItemStack> = mutableListOf()
    //all the stacks the system will have to craft to fullfill the requirements (used in request screen & crafting computer)
    val craftStacks: MutableList<ItemStack> = mutableListOf()
    //stacks that couldn't be previously inserted in a machine due to it being full (used in crafting computer)
    val failedStacks: MutableList<ItemStack> = mutableListOf()
    //stacks that are going to be used for crafting
    val inputStacks: MutableList<ItemStack> = mutableListOf()
    //stacks that are going to get crafted
    val outputStacks: MutableList<ItemStack> = mutableListOf()

    var craftableInfo = network?.getCraftableInfo(craftStack)
    var machinePos = craftableInfo?.second
    var tag = craftableInfo?.first
    var type: Type = if(tag?.getString("type") == "crafting") Type.CRAFTING else Type.PROCESSING
    var useNbt: Boolean = tag?.getBoolean("useNbt") ?: false
    var state = State.NOTHING
    var quantity = 0

    companion object {
        fun fromTag(tag: CompoundTag): CraftingAction {
            val action = CraftingAction(null, mutableListOf(), mutableListOf(), null, getStackFromTag(tag.getCompound("craftStack")), 0)
            (tag.get("necessaryActions") as ListTag).forEach {
                action.necessaryActions.add(fromTag(it as CompoundTag))
            }
            (tag.get("missingTags") as ListTag).forEach {
                action.missingTags.add(TagStack.fromTag(it as CompoundTag))
            }
            (tag.get("missingStacks") as ListTag).forEach {
                action.missingStacks.add(getStackFromTag(it as CompoundTag))
            }
            (tag.get("inputStacks") as ListTag).forEach {
                action.availableStacks.add(getStackFromTag(it as CompoundTag))
            }
            (tag.get("outputStacks") as ListTag).forEach {
                action.outputStacks.add(getStackFromTag(it as CompoundTag))
            }
            val info = Pair(tag.getCompound("craftableInfoTag"), BlockPos.fromLong(tag.getLong("craftableInfoBlockPos")))
            action.craftableInfo = info
            action.tag = info.first
            action.machinePos = info.second
            action.type = if(tag.getString("type") == "crafting") Type.CRAFTING else Type.PROCESSING
            action.useNbt = tag.getBoolean("useNbt")
            action.quantity = tag.getInt("quantity")
            return action
        }
    }

    fun toTag(tag: CompoundTag): CompoundTag {
        tag.put("craftStack", getTagFromStack(craftStack))
        tag.putInt("quantity", quantity)
        val necessaryActionsListTag = ListTag()
        necessaryActions.forEach {
            necessaryActionsListTag.add(it.toTag(CompoundTag()))
        }
        tag.put("necessaryActions", necessaryActionsListTag)
        val missingTagsListTag = ListTag()
        missingTags.forEach {
            missingTagsListTag.add(it.toTag(CompoundTag()))
        }
        tag.put("missingTags", missingTagsListTag)
        val missingStacksListTag = ListTag()
        missingStacks.forEach {
            missingStacksListTag.add(getTagFromStack(it))
        }
        tag.put("missingStacks", missingStacksListTag)
        val inputStacksListTag = ListTag()
        availableStacks.forEach {
            inputStacksListTag.add(getTagFromStack(it))
        }
        tag.put("inputStacks", inputStacksListTag)
        val outputStacksListTag = ListTag()
        outputStacks.forEach {
            outputStacksListTag.add(getTagFromStack(it))
        }
        tag.put("outputStacks", outputStacksListTag)
        craftableInfo?.let {
            tag.put("craftableInfoTag", it.first)
            tag.putLong("craftableInfoBlockPos", it.second.asLong())
        }

        return tag
    }

    init {
        // outputssss
        if(tag?.get("output") is CompoundTag)
            outputStacks.add(ItemStack.fromTag(tag!!.getCompound("output")))
        if(tag?.get("output") is ListTag) {
            val outputList = tag!!.get("output") as ListTag
            outputList.forEach {
                outputStacks.add(ItemStack.fromTag(it as CompoundTag))
            }
        }

        outputStacks.forEach {
            if(areStacksCompatible(it, craftStack)) {
                quantity = MathHelper.ceil(requestedQuantity / it.count.toFloat())
                return@forEach
            }
        }

        // inputssss
        val missingButCraftable = mutableMapOf<ItemStack, Int>()

        val listTag = tag?.get("input") as ListTag?
        listTag?.forEach {tag ->
            var stack = ItemStack.EMPTY
            if (tag is StringTag) {
                val helper = IngredientHelper(tag.asString())
                if(helper.type == IngredientHelper.Type.TAG) {
                    val tagStack = TagStack(helper.tag!!)
                    var missing = quantity
                    tagStack.getStacks(world!!).forEach {stk ->
                        inputStacks.add(stk.copy())
                        stacks.forEach stored@{ storedStack ->
                            if(missing > 0 && areStacksCompatible(stk, storedStack)) {
                                availableStacks.add(stk.copy())
                                val aux = missing
                                missing -= storedStack.count
                                if(missing >= 0)
                                    storedStack.decrement(storedStack.count)
                                if(missing < 0) {
                                    storedStack.decrement(aux)
                                    missing = 0
                                }
                                return@stored
                            }
                        }
                        if(missing > 0) {
                            craftables.forEach craftable@{ craftableStack ->
                                var overflow = false
                                outputStacks.forEach {
                                    if(areStacksCompatible(it, craftableStack)) overflow = true
                                }
                                if(!overflow && areStacksCompatible(stk, craftableStack)) {
                                    var found = false
                                    craftStacks.add(stack.copy())
                                    val it = missingButCraftable.iterator()
                                    while(it.hasNext()) {
                                        val entry = it.next()
                                        val qnt = entry.value
                                        if(areStacksCompatible(stk, entry.key)) {
                                            it.remove()
                                            missingButCraftable[stk] = qnt+missing
                                            found = true
                                            break
                                        }
                                    }
                                    if(!found) missingButCraftable[stk] = missing
                                    missing = 0
                                    return@craftable
                                }
                            }
                        }
                    }
                    if(missing > 0) {
                        tagStack.count *= missing
                        missingTags.add(tagStack)
                    }
                }
                if(helper.type == IngredientHelper.Type.ITEM) stack = ItemStack(helper.item!!)

            }
            if (tag is CompoundTag) {
                stack = getStackFromTag(tag)
            }
            if(!stack.isEmpty) {
                inputStacks.add(stack.copy())
                var missing = quantity
                stacks.forEach stored@{ storedStack ->
                    if(missing > 0 && areStacksCompatible(stack, storedStack)) {
                        availableStacks.add(stack.copy())
                        val aux = missing
                        missing -= storedStack.count
                        if(missing >= 0)
                            storedStack.decrement(storedStack.count)
                        if(missing < 0) {
                            storedStack.decrement(aux)
                            missing = 0
                        }
                        return@stored
                    }
                }
                if(missing > 0) {
                    craftables.forEach craftable@{ craftableStack ->
                        var overflow = false
                        outputStacks.forEach {
                            if(areStacksCompatible(it, craftableStack)) overflow = true
                        }
                        if(!overflow && areStacksCompatible(stack, craftableStack)) {
                            var found = false
                            craftStacks.add(stack.copy())
                            val it = missingButCraftable.iterator()
                            while(it.hasNext()) {
                                val entry = it.next()
                                val qnt = entry.value
                                if(areStacksCompatible(stack, entry.key)) {
                                    it.remove()
                                    missingButCraftable[stack] = qnt+missing
                                    found = true
                                    break;
                                }
                            }
                            if(!found) missingButCraftable[stack] = missing
                            missing = 0
                            return@craftable
                        }
                    }
                }
                if(missing > 0) {
                    val dummyStack = stack.copy()
                    dummyStack.count *= missing
                    missingStacks.add(dummyStack)
                }
            }
        }

        missingButCraftable.forEach {
            craftables.forEach {crfStk ->
                if(areStacksCompatible(it.key, crfStk)) {
                    necessaryActions.add(CraftingAction(network, stacks, craftables, world, it.key, it.value))
                }
            }

        }

    }

}