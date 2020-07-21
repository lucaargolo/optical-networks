package io.github.lucaargolo.opticalnetworks.blocks

import io.github.lucaargolo.opticalnetworks.utils.ModBlockItem
import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

open class ModBlock(block: Block) {

    var block: Block = block
        private set

    open fun init(identifier: Identifier) {
        Registry.register(Registry.BLOCK, identifier, block)
        Registry.register(Registry.ITEM, identifier, ModBlockItem(block, Item.Settings().group(ItemGroup.MISC)))
    }

    open fun initClient(identifier: Identifier) {}
}