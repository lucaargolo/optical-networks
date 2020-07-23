package io.github.lucaargolo.opticalnetworks.utils

import io.github.lucaargolo.opticalnetworks.network.blocks.CableConnectable
import net.minecraft.block.Block
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.World

class ModBlockItem(block: Block, settings: Settings): BlockItem(block, settings) {

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        (block as? CableConnectable)?.let {
            if(it.energyUsage > 0) tooltip.add(LiteralText("${Formatting.BLUE}Energy usage: ${Formatting.GRAY}${it.energyUsage} E/t"))
            if(it.bandwidthUsage > 0) tooltip.add(LiteralText("${Formatting.BLUE}Bandwidth usage: ${Formatting.GRAY}${it.bandwidthUsage}"))
        }
    }

}