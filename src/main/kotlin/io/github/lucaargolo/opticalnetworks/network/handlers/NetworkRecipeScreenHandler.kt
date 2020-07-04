package io.github.lucaargolo.opticalnetworks.network.handlers

import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.network.entity.NetworkBlockEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.screen.AbstractRecipeScreenHandler
import net.minecraft.screen.ScreenHandlerContext

abstract class NetworkRecipeScreenHandler<C: Inventory>(syncId: Int, playerInventory: PlayerInventory, val network: Network, val entity: NetworkBlockEntity, val context: ScreenHandlerContext): AbstractRecipeScreenHandler<C>(null, syncId)
