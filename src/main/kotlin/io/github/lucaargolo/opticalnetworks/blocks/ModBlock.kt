package io.github.lucaargolo.opticalnetworks.blocks

import io.github.lucaargolo.opticalnetworks.CREATIVE_TAB
import io.github.lucaargolo.opticalnetworks.utils.ModBlockItem
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

open class ModBlock(block: Block, private val hasModBlock: Boolean) {

    constructor(block: Block): this(block, true)

    var block: Block = block
        private set

    open fun init(identifier: Identifier) {
        Registry.register(Registry.BLOCK, identifier, block)
        if(hasModBlock)
            Registry.register(Registry.ITEM, identifier, ModBlockItem(block, Item.Settings().group(CREATIVE_TAB)))
    }

    open fun initClient(identifier: Identifier) {}
}