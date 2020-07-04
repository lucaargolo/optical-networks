package io.github.lucaargolo.opticalnetworks.utils

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.recipe.Ingredient
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

class TagStack(var mcTag: String, var count: Int) {

    constructor(mcTag: String): this(mcTag, 1)

    var renderStacks: List<ItemStack> = listOf()

    init {
        try {
            val ingredient = Ingredient.fromJson(JsonParser().parse("{ \"tag\": \"${mcTag}\" }"))
            renderStacks = ingredient.matchingStacksClient.asList()
        }catch(ignored: Exception) {}
    }

    fun toTag(tag: CompoundTag): CompoundTag {
        tag.putString("mcTag", mcTag)
        tag.putInt("count", count)
        return tag
    }

    fun copy() = TagStack(mcTag, count)

    companion object {
        fun fromTag(tag: CompoundTag): TagStack {
            return TagStack(tag.getString("mcTag"), tag.getInt("count"))
        }
    }

    fun getStacks(world: ServerWorld): MutableList<ItemStack> {
        val inputList = mutableListOf<ItemStack>()
        world.server.tagManager?.items()?.get(Identifier(mcTag))?.values()?.forEach { item ->
             val stk = ItemStack(item)
             if (!stk.isEmpty) inputList.add(stk)
        }
        return inputList
    }

}