package io.github.lucaargolo.opticalnetworks.items.basic

import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.world.World

class CraftingMemory(settings: Settings, val space: Int): Item(settings) {

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        val speedText = TranslatableText("tooltip.opticalnetworks.space")
        speedText.append(space.toString())
        tooltip.add(speedText)
    }

}