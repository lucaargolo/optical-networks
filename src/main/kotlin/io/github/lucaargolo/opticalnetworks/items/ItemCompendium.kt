package io.github.lucaargolo.opticalnetworks.items

import io.github.lucaargolo.opticalnetworks.CREATIVE_TAB
import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.items.basic.*
import io.github.lucaargolo.opticalnetworks.items.blueprint.Blueprint
import net.minecraft.item.FoodComponent
import net.minecraft.item.FoodComponents
import net.minecraft.item.Item
import net.minecraft.item.Item.Settings
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

val itemRegistry = mutableMapOf<Identifier, ModItem>()

val BANANA = register(Identifier(MOD_ID, "banana"), ModItem(Banana(Settings().group(CREATIVE_TAB).food(FoodComponents.APPLE))))
val BANANA_PEEL = register(Identifier(MOD_ID, "banana_peel"), ModItem(Item(Settings().group(CREATIVE_TAB))))

val RAW_BIOPLASTIC = register(Identifier(MOD_ID, "raw_bioplastic"), ModItem(Item(Settings().group(CREATIVE_TAB))))
val BIOPLASTIC = register(Identifier(MOD_ID, "bioplastic"), ModItem(Item(Settings().group(CREATIVE_TAB))))
val BASIC_DISC = register(Identifier(MOD_ID, "basic_disc"), ModItem(Item(Settings().group(CREATIVE_TAB))))

val WRENCH = register(Identifier(MOD_ID, "wrench"), ModItem(Wrench(Settings().group(CREATIVE_TAB).maxCount(1))))
val NETWORK_ANALYSER = register(Identifier(MOD_ID, "network_analyser"), ModItem(NetworkAnalyser(Settings().group(CREATIVE_TAB).maxCount(1))))
val BLUEPRINT = register(Identifier(MOD_ID, "blueprint"), ModItem(Blueprint(Settings().group(CREATIVE_TAB))))

val LED = register(Identifier(MOD_ID, "led"), ModItem(Item(Settings().group(CREATIVE_TAB))))
val RED_LED = register(Identifier(MOD_ID, "red_led"), ModItem(Item(Settings().group(CREATIVE_TAB))))
val GREEN_LED = register(Identifier(MOD_ID, "green_led"), ModItem(Item(Settings().group(CREATIVE_TAB))))
val BLUE_LED = register(Identifier(MOD_ID, "blue_led"), ModItem(Item(Settings().group(CREATIVE_TAB))))
val SCREEN_COMPONENT = register(Identifier(MOD_ID, "screen_component"), ModItem(Item(Settings().group(CREATIVE_TAB))))
val CONSTRUCTION_COMPONENT = register(Identifier(MOD_ID, "construction_component"), ModItem(Item(Settings().group(CREATIVE_TAB))))
val DESTRUCTION_COMPONENT = register(Identifier(MOD_ID, "destruction_component"), ModItem(Item(Settings().group(CREATIVE_TAB))))
val MICROCHIP = register(Identifier(MOD_ID, "microchip"), ModItem(Item(Settings().group(CREATIVE_TAB))))
val ADVANCED_MICROCHIP = register(Identifier(MOD_ID, "advanced_microchip"), ModItem(Item(Settings().group(CREATIVE_TAB))))

val ITEM_DRIVE_1K = register(Identifier(MOD_ID, "item_drive_1k"), ModItem(ItemDrive(Settings().group(CREATIVE_TAB).maxCount(1), 1024)))
val ITEM_DRIVE_4K = register(Identifier(MOD_ID, "item_drive_4k"), ModItem(ItemDrive(Settings().group(CREATIVE_TAB).maxCount(1), 4096)))
val ITEM_DRIVE_16K = register(Identifier(MOD_ID, "item_drive_16k"), ModItem(ItemDrive(Settings().group(CREATIVE_TAB).maxCount(1), 16364)))
val ITEM_DRIVE_64K = register(Identifier(MOD_ID, "item_drive_64k"), ModItem(ItemDrive(Settings().group(CREATIVE_TAB).maxCount(1), 65536)))

val CRAFTING_MEMORY_MK1 = register(Identifier(MOD_ID, "crafting_memory_mk1"), ModItem(CraftingMemory(Settings().group(CREATIVE_TAB).maxCount(1), 8)))
val CRAFTING_MEMORY_MK2 = register(Identifier(MOD_ID, "crafting_memory_mk2"), ModItem(CraftingMemory(Settings().group(CREATIVE_TAB).maxCount(1), 16)))
val CRAFTING_MEMORY_MK3 = register(Identifier(MOD_ID, "crafting_memory_mk3"), ModItem(CraftingMemory(Settings().group(CREATIVE_TAB).maxCount(1), 32)))
val CRAFTING_MEMORY_MK4 = register(Identifier(MOD_ID, "crafting_memory_mk4"), ModItem(CraftingMemory(Settings().group(CREATIVE_TAB).maxCount(1), 64)))

val AND_CPU_MK1 = register(Identifier(MOD_ID, "and_cpu_mk1"), ModItem(CraftingProcessingUnit(Settings().group(CREATIVE_TAB).maxCount(1), 2, 0.25F)))
val AND_CPU_MK2 = register(Identifier(MOD_ID, "and_cpu_mk2"), ModItem(CraftingProcessingUnit(Settings().group(CREATIVE_TAB).maxCount(1), 2, 0.5F)))
val AND_CPU_MK3 = register(Identifier(MOD_ID, "and_cpu_mk3"), ModItem(CraftingProcessingUnit(Settings().group(CREATIVE_TAB).maxCount(1), 4, 1.0F)))

val INT_CPU_MK1 = register(Identifier(MOD_ID, "int_cpu_mk1"), ModItem(CraftingProcessingUnit(Settings().group(CREATIVE_TAB).maxCount(1), 1, 0.5F)))
val INT_CPU_MK2 = register(Identifier(MOD_ID, "int_cpu_mk2"), ModItem(CraftingProcessingUnit(Settings().group(CREATIVE_TAB).maxCount(1), 1, 1.0F)))
val INT_CPU_MK3 = register(Identifier(MOD_ID, "int_cpu_mk3"), ModItem(CraftingProcessingUnit(Settings().group(CREATIVE_TAB).maxCount(1), 2, 2.0F)))

val CREATIVE_CPU = register(Identifier(MOD_ID, "creative_cpu"), ModItem(CraftingProcessingUnit(Settings().group(CREATIVE_TAB).maxCount(1), 256, 20.0F)))

//val BLANK_ADDON = register(Identifier(MOD_ID, "blank_addon"), ModItem(Addon(Settings().group(CREATIVE_TAB))))
//val REDSTONE_ADDON = register(Identifier(MOD_ID, "redstone_addon"), ModItem(Addon(Settings().group(CREATIVE_TAB))))
//val COLOR_ADDON = register(Identifier(MOD_ID, "color_addon"), ModItem(Addon(Settings().group(CREATIVE_TAB))))
//val SPEED_ADDON = register(Identifier(MOD_ID, "speed_addon"), ModItem(Addon(Settings().group(CREATIVE_TAB))))
//val ENERGY_ADDON = register(Identifier(MOD_ID, "energy_addon"), ModItem(Addon(Settings().group(CREATIVE_TAB))))
//val BANDWIDTH_ADDON = register(Identifier(MOD_ID, "bandwidth_addon"), ModItem(Addon(Settings().group(CREATIVE_TAB))))
//
//val ADVANCED_BLANK_ADDON = register(Identifier(MOD_ID, "advanced_blank_addon"), ModItem(Addon(Settings().group(CREATIVE_TAB))))
//val ADVANCED_SPEED_ADDON = register(Identifier(MOD_ID, "advanced_speed_addon"), ModItem(Addon(Settings().group(CREATIVE_TAB))))
//val ADVANCED_ENERGY_ADDON = register(Identifier(MOD_ID, "advanced_energy_addon"), ModItem(Addon(Settings().group(CREATIVE_TAB))))
//val ADVANCED_BANDWIDTH_ADDON = register(Identifier(MOD_ID, "advanced_bandwidth_addon"), ModItem(Addon(Settings().group(CREATIVE_TAB))))

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
