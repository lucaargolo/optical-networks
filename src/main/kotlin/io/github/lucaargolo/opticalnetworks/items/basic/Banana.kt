package io.github.lucaargolo.opticalnetworks.items.basic

import io.github.lucaargolo.opticalnetworks.items.BANANA_PEEL
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.world.World

class Banana(settings: Settings): Item(settings) {

    override fun finishUsing(stack: ItemStack?, world: World?, user: LivingEntity?): ItemStack {
        (user as? ServerPlayerEntity)?.inventory?.offerOrDrop(world, ItemStack(BANANA_PEEL))
        return super.finishUsing(stack, world, user)
    }

}