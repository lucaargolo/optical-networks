package io.github.lucaargolo.opticalnetworks.items.basic

import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.ListTag
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.world.World

class CraftingProcessingUnit(settings: Settings, val cores: Int, val speed: Float): Item(settings) {

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val coresText = TranslatableText("tooltip.opticalnetworks.cores")
        coresText.append(cores.toString())
        tooltip.add(coresText)
        val speedText = TranslatableText("tooltip.opticalnetworks.speed")
        speedText.append(speed.toString())
        tooltip.add(speedText)
    }

}