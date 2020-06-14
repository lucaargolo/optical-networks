package io.github.lucaargolo.opticalnetworks.utils

import io.github.lucaargolo.opticalnetworks.network.Network
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext

abstract class NetworkScreenHandler(syncId: Int, playerInventory: PlayerInventory, val network: Network, val context: ScreenHandlerContext): ScreenHandler(null, syncId)
