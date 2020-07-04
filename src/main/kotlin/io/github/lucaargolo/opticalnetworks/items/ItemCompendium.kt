package io.github.lucaargolo.opticalnetworks.items

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.items.basic.CraftingDisc
import io.github.lucaargolo.opticalnetworks.items.basic.CraftingProcessingUnit
import io.github.lucaargolo.opticalnetworks.items.blueprint.Blueprint
import io.github.lucaargolo.opticalnetworks.items.basic.ItemDisc
import io.github.lucaargolo.opticalnetworks.items.basic.NetworkAnalyser
import net.minecraft.item.Item
import net.minecraft.item.Item.Settings
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

val itemRegistry = mutableMapOf<Identifier, ModItem>()

val NETWORK_ANALYSER = register(Identifier(MOD_ID, "network_analyser"), ModItem(NetworkAnalyser(Settings().group(ItemGroup.MISC))))

val BLUEPRINT = register(Identifier(MOD_ID, "blueprint"), ModItem(Blueprint(Settings().group(ItemGroup.MISC))))
val DISC_4K = register(Identifier(MOD_ID, "disc_4k"), ModItem(Item(Settings().group(ItemGroup.MISC))))
val ITEM_DISC_4K = register(Identifier(MOD_ID, "item_disc_4k"), ModItem(ItemDisc(Settings().group(ItemGroup.MISC).maxCount(1), 4096)))
val CRAFTING_DISC_4K = register(Identifier(MOD_ID, "crafting_disc_4k"), ModItem(CraftingDisc(Settings().group(ItemGroup.MISC).maxCount(1), 4)))

val AND_CPU_MK1 = register(Identifier(MOD_ID, "and_cpu_mk1"), ModItem(CraftingProcessingUnit(Settings().group(ItemGroup.MISC).maxCount(1), 2, 0.25F)))
val AND_CPU_MK2 = register(Identifier(MOD_ID, "and_cpu_mk2"), ModItem(CraftingProcessingUnit(Settings().group(ItemGroup.MISC).maxCount(1), 2, 0.5F)))
val AND_CPU_MK3 = register(Identifier(MOD_ID, "and_cpu_mk3"), ModItem(CraftingProcessingUnit(Settings().group(ItemGroup.MISC).maxCount(1), 4, 1.0F)))

val INT_CPU_MK1 = register(Identifier(MOD_ID, "int_cpu_mk1"), ModItem(CraftingProcessingUnit(Settings().group(ItemGroup.MISC).maxCount(1), 1, 0.5F)))
val INT_CPU_MK2 = register(Identifier(MOD_ID, "int_cpu_mk2"), ModItem(CraftingProcessingUnit(Settings().group(ItemGroup.MISC).maxCount(1), 1, 1.0F)))
val INT_CPU_MK3 = register(Identifier(MOD_ID, "int_cpu_mk3"), ModItem(CraftingProcessingUnit(Settings().group(ItemGroup.MISC).maxCount(1), 2, 2.0F)))

val CREATIVE_CPU = register(Identifier(MOD_ID, "creative_cpu"), ModItem(CraftingProcessingUnit(Settings().group(ItemGroup.MISC).maxCount(1), 256, 20.0F)))

private fun register(identifier: Identifier, item: ModItem): Item {
    itemRegistry[identifier] = item
    return item.item
}

fun getItemId(item: Item): Identifier? {
    itemRegistry.forEach {
        if(it.value.item == item) return it.key
    }
    return null
}

fun initItems() {
    itemRegistry.forEach{ it.value.init(it.key) }
}

fun initItemsClient() {
    itemRegistry.forEach{ it.value.initClient(it.key) }
}
