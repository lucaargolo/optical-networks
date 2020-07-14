package io.github.lucaargolo.opticalnetworks.items.basic

import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.world.World

class ItemDrive(settings: Settings, val space: Int): Item(settings) {

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        val tag = stack.orCreateTag;
        var used = 0;
        if(tag.contains("items")) {
            (tag.get("items") as ListTag).forEach {
                used += (it as CompoundTag).getInt("Count")
            }
        }
        tooltip.add(LiteralText("Space: $used/$space"))
        super.appendTooltip(stack, world, tooltip, context)
    }

}