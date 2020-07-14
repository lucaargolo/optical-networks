package io.github.lucaargolo.opticalnetworks.items

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.items.basic.CraftingMemory
import io.github.lucaargolo.opticalnetworks.items.basic.CraftingProcessingUnit
import io.github.lucaargolo.opticalnetworks.items.blueprint.Blueprint
import io.github.lucaargolo.opticalnetworks.items.basic.ItemDrive
import io.github.lucaargolo.opticalnetworks.items.basic.NetworkAnalyser
import net.minecraft.item.Item
import net.minecraft.item.Item.Settings
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

val itemRegistry = mutableMapOf<Identifier, ModItem>()

val NETWORK_ANALYSER = register(Identifier(MOD_ID, "network_analyser"), ModItem(NetworkAnalyser(Settings().group(ItemGroup.MISC))))

val BLUEPRINT = register(Identifier(MOD_ID, "blueprint"), ModItem(Blueprint(Settings().group(ItemGroup.MISC))))
val BASIC_DISC = register(Identifier(MOD_ID, "basic_disc"), ModItem(Item(Settings().group(ItemGroup.MISC))))

val ITEM_DRIVE_1K = register(Identifier(MOD_ID, "item_drive_1k"), ModItem(ItemDrive(Settings().group(ItemGroup.MISC).maxCount(1), 1024)))
val ITEM_DRIVE_4K = register(Identifier(MOD_ID, "item_drive_4k"), ModItem(ItemDrive(Settings().group(ItemGroup.MISC).maxCount(1), 4096)))
val ITEM_DRIVE_16K = register(Identifier(MOD_ID, "item_drive_16k"), ModItem(ItemDrive(Settings().group(ItemGroup.MISC).maxCount(1), 16364)))
val ITEM_DRIVE_64K = register(Identifier(MOD_ID, "item_drive_64k"), ModItem(ItemDrive(Settings().group(ItemGroup.MISC).maxCount(1), 65536)))

val CRAFTING_MEMORY_MK1 = register(Identifier(MOD_ID, "crafting_memory_mk1"), ModItem(CraftingMemory(Settings().group(ItemGroup.MISC).maxCount(1), 1)))
val CRAFTING_MEMORY_MK2 = register(Identifier(MOD_ID, "crafting_memory_mk2"), ModItem(CraftingMemory(Settings().group(ItemGroup.MISC).maxCount(1), 2)))
val CRAFTING_MEMORY_MK3 = register(Identifier(MOD_ID, "crafting_memory_mk3"), ModItem(CraftingMemory(Settings().group(ItemGroup.MISC).maxCount(1), 3)))
val CRAFTING_MEMORY_MK4 = register(Identifier(MOD_ID, "crafting_memory_mk4"), ModItem(CraftingMemory(Settings().group(ItemGroup.MISC).maxCount(1), 4)))

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
