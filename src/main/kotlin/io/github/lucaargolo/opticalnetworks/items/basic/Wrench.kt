package io.github.lucaargolo.opticalnetworks.items.basic

import io.github.lucaargolo.opticalnetworks.blocks.`interface`.Interface
import net.minecraft.item.Item
import net.minecraft.item.ItemUsageContext
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult

class Wrench(settings: Settings): Item(settings) {

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val state = context.world.getBlockState(context.blockPos)
        if(state.contains(Interface.DIRECTIONAL) && !state[Interface.DIRECTIONAL]) {
            context.world.setBlockState(context.blockPos, state.with(Interface.DIRECTIONAL, true))
            return ActionResult.SUCCESS
        }
        if(state.contains(Properties.FACING)) {
            if(context.player?.isSneaking == true)
                context.world.setBlockState(context.blockPos, state.with(Properties.FACING, context.side))
            else
                context.world.setBlockState(context.blockPos, state.with(Properties.FACING, context.side.opposite))
            return ActionResult.SUCCESS
        }
        if(state.contains(Properties.HORIZONTAL_FACING)) {
            if(context.player?.isSneaking == true)
                context.world.setBlockState(context.blockPos, state.with(Properties.HORIZONTAL_FACING, context.playerFacing))
            else
                context.world.setBlockState(context.blockPos, state.with(Properties.HORIZONTAL_FACING, context.playerFacing.opposite))
            return ActionResult.SUCCESS
        }
        return super.useOnBlock(context)
    }

}