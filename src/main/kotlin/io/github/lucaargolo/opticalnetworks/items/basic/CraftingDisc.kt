package io.github.lucaargolo.opticalnetworks.items.basic

import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.world.World

class CraftingDisc(settings: Settings, val space: Int): Item(settings) {

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        val tag = stack.orCreateTag;
        var used = 0;
        if(tag.contains("queue")) {
            (tag.get("queue") as ListTag).forEach { used++ }
        }
        tooltip.add(LiteralText("Bytes: $used/$space"))
        super.appendTooltip(stack, world, tooltip, context)
    }

}