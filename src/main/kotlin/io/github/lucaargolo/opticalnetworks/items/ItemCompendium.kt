package io.github.lucaargolo.opticalnetworks.items

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.items.basic.DiscDrive
import io.github.lucaargolo.opticalnetworks.items.basic.NetworkAnalyser
import net.minecraft.item.Item
import net.minecraft.item.Item.Settings
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

val itemRegistry = mutableMapOf<Identifier, ModItem>()

val NETWORK_ANALYSER = register(Identifier(MOD_ID, "network_analyser"), ModItem(NetworkAnalyser(Settings().group(ItemGroup.MISC))))

val DISC_4K = register(Identifier(MOD_ID, "disc_4k"), ModItem(Item(Settings().group(ItemGroup.MISC))))
val DISC_DRIVE_4K = register(Identifier(MOD_ID, "disc_drive_4k"), ModItem(DiscDrive(Settings().group(ItemGroup.MISC).maxCount(1), 4096)))

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
