package io.github.lucaargolo.opticalnetworks.utils

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.network.NetworkState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.registry.Registry

fun getNetworkState(world: ServerWorld): NetworkState {
    return world.persistentStateManager.getOrCreate( { NetworkState(world) }, MOD_ID)
}

fun getStackFromTag(tag: CompoundTag): ItemStack {
    val dummyTag = tag.copy();
    dummyTag.putByte("Count", 1)
    val stack = ItemStack.fromTag(dummyTag)
    stack.count = tag.getInt("Count")
    return stack;
}

fun getTagFromStack(stack: ItemStack): CompoundTag {
    val tag = CompoundTag()
    val identifier = Registry.ITEM.getId(stack.item)
    tag.putString("id", identifier.toString())
    tag.putInt("Count", stack.count)
    if (stack.tag != null) {
        tag.put("tag", stack.tag!!.copy())
    }
    return tag
}

fun areStacksCompatible(left: ItemStack, right: ItemStack): Boolean {
    return ItemStack.areItemsEqual(left, right) && ItemStack.areTagsEqual(left, right)
}


