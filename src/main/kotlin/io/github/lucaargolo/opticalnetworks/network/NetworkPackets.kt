package io.github.lucaargolo.opticalnetworks.network

import com.google.common.collect.Lists
import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.blocks.controller.ControllerBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.terminal.BlueprintTerminalBlockEntity
import io.github.lucaargolo.opticalnetworks.blocks.terminal.BlueprintTerminalScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.terminal.CraftingTerminalScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.terminal.TerminalConfig
import io.github.lucaargolo.opticalnetworks.mixed.ServerPlayerEntityMixed
import io.github.lucaargolo.opticalnetworks.utils.GhostSlot
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketContext
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.LiteralText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.awt.Color

val NETWORK_INTERACT_C2S_PACKET = Identifier(MOD_ID, "network_interact")
val GHOST_SLOT_CLICK_C2S_PACKET = Identifier(MOD_ID, "ghost_slot_click")
val UPDATE_CABLE_BUTTONS_C2S_PACKET = Identifier(MOD_ID, "update_cable_buttons")
val UPDATE_TERMINAL_BUTTONS_C2S_PACKET = Identifier(MOD_ID, "update_terminal_buttons")
val UPDATE_TERMINAL_CONFIG_C2S_PACKET = Identifier(MOD_ID, "update_terminal_config_c2s")
val CLEAR_TERMINAL_TABLE = Identifier(MOD_ID, "clear_terminal_item")
val MOVE_TERMINAL_ITEMS_PACKET = Identifier(MOD_ID, "move_terminal_items")

fun initNetworkPackets() {

    ServerSidePacketRegistry.INSTANCE.register(UPDATE_TERMINAL_CONFIG_C2S_PACKET) { packetContext: PacketContext, attachedData: PacketByteBuf ->
        packetContext.run {
            attachedData.readCompoundTag()?.let {  (packetContext.player as ServerPlayerEntityMixed).`opticalNetworks$terminalConfig`.fromTag(it) }
        }
    }

    ServerSidePacketRegistry.INSTANCE.register(NETWORK_INTERACT_C2S_PACKET) { packetContext: PacketContext, attachedData: PacketByteBuf ->

        val player = packetContext.player
        val playerInventory = player.inventory
        val networkId = attachedData.readUuid()
        val networkState = getNetworkState(player.world as ServerWorld)
        val network = networkState.getNetworkByUUID(networkId)
        if(network != null) {

            val type = attachedData.readInt()

            when(type) {
                1 -> {
                    //mouseClicked in TerminalScreen
                    val button = attachedData.readInt()
                    val shift = attachedData.readBoolean()
                    val stack = attachedData.readItemStack()
                    packetContext.taskQueue.execute {
                        executeMouseClicker(stack, playerInventory, shift, network, button)
                    }
                }
                2 -> {
                    //change controller color
                    val color = attachedData.readString()
                    packetContext.taskQueue.execute {
                        changeColor(color, player, network)
                    }
                }
            }
        }
    }

    ServerSidePacketRegistry.INSTANCE.register(GHOST_SLOT_CLICK_C2S_PACKET) { packetContext: PacketContext, attachedData: PacketByteBuf ->

        val pos = attachedData.readBlockPos()
        val stk = attachedData.readItemStack()
        val idx = attachedData.readInt()

        packetContext.taskQueue.execute {
            val entity = packetContext.player.world.getBlockEntity(pos)
            if(entity is GhostSlot.GhostSlotBlockEntity) entity.ghostInv[idx] = stk
            if(entity is BlockEntityClientSerializable) entity.sync()
            packetContext.player.currentScreenHandler.onContentChanged(null)
        }
    }

    ServerSidePacketRegistry.INSTANCE.register(UPDATE_CABLE_BUTTONS_C2S_PACKET) { packetContext: PacketContext, attachedData: PacketByteBuf ->

        val pos = attachedData.readBlockPos()
        val e1 = attachedData.readInt()
        val e2 = attachedData.readInt()
        val e3 = attachedData.readInt()
        val e4 = attachedData.readInt()
        val e5 = attachedData.readInt()

        packetContext.taskQueue.execute {
            val state = packetContext.player.world.getBlockState(pos)
            val entity = packetContext.player.world.getBlockEntity(pos)
            val tag = entity?.toTag(CompoundTag())
            tag?.let {
                it.putInt("listMode", e1)
                it.putInt("nbtMode", e2)
                it.putInt("damageMode", e3)
                it.putInt("orderMode", e4)
                it.putInt("redstoneMode", e5)
            }
            tag?.let { entity.fromTag(state, tag) }
            if(entity is BlockEntityClientSerializable) entity.sync()
        }
    }

    ServerSidePacketRegistry.INSTANCE.register(UPDATE_TERMINAL_BUTTONS_C2S_PACKET) { packetContext: PacketContext, attachedData: PacketByteBuf ->

        val e1 = attachedData.readInt()
        val e2 = attachedData.readInt()
        val e3 = attachedData.readInt()

        packetContext.taskQueue.execute {
            val tag = packetContext.player.toTag(CompoundTag())
            tag.putInt("terminalSize", e1)
            tag.putInt("terminalSort", e2)
            tag.putInt("terminalSortDirection", e3)
            packetContext.player.fromTag(tag)
        }
    }

    ServerSidePacketRegistry.INSTANCE.register(CLEAR_TERMINAL_TABLE) { packetContext: PacketContext, _ ->
        val screenHandler = packetContext.player.currentScreenHandler
        if(screenHandler is CraftingTerminalScreenHandler) {
            packetContext.taskQueue.execute {
                clearTerminalTable(screenHandler, packetContext.player as ServerPlayerEntity)
            }
        }else if(screenHandler is BlueprintTerminalScreenHandler) {
            packetContext.taskQueue.execute {
                (screenHandler.entity as BlueprintTerminalBlockEntity).ghostInv.clear()
                screenHandler.entity.markDirty()
                screenHandler.entity.sync()
            }
        }
    }

    if(!FabricLoader.getInstance().isModLoaded("roughlyenoughitems")) return

    ServerSidePacketRegistry.INSTANCE.register(MOVE_TERMINAL_ITEMS_PACKET) { packetContext: PacketContext, packetByteBuf: PacketByteBuf ->
        val category = packetByteBuf.readIdentifier()
        val player = packetContext.player as ServerPlayerEntity
        val container = player.currentScreenHandler as CraftingTerminalScreenHandler
        val playerContainer = player.playerScreenHandler
        val shift = packetByteBuf.readBoolean()
        val input = mutableMapOf<Int, List<ItemStack>>()
        val mapSize = packetByteBuf.readInt()
        for (i in 0 until mapSize) {
            val list: MutableList<ItemStack> = Lists.newArrayList()
            val count = packetByteBuf.readInt()
            for (j in 0 until count) {
                list.add(packetByteBuf.readItemStack())
            }
            input[i] = list
        }
        packetContext.taskQueue.execute {
            clearTerminalTable(container, player)
            val networkItems = container.network.searchStacks("");
            input.forEach { (craftingSlotId, possibleStacks) ->
                val possibleItems = mutableListOf<Item>()
                possibleStacks.forEach {
                    possibleItems.add(it.item)
                }
                networkItems.forEach net@{ networkStack ->
                    if(possibleItems.contains(networkStack.item)) {
                        possibleStacks.forEach { requiredStack ->
                            if(areStacksCompatible(networkStack, requiredStack)) {
                                container.network.removeStack(requiredStack.copy())
                                container.slots[craftingSlotId+1].stack = requiredStack
                                player.networkHandler.sendPacket(ScreenHandlerSlotUpdateS2CPacket(container.syncId, craftingSlotId+1, requiredStack))
                                return@net
                            }
                        }
                    }
                }
            }
        }
        println(input)
    }

}

private fun clearTerminalTable(screenHandler: CraftingTerminalScreenHandler, player: ServerPlayerEntity) {
    val pair = screenHandler.network.getSpace()
    val availableSpace = pair.second-pair.first
    player.networkHandler.sendPacket(ScreenHandlerSlotUpdateS2CPacket(screenHandler.syncId, 0, ItemStack.EMPTY))
    (1..10).forEach {
        val stk = screenHandler.slots[it].stack.copy()
        if(!stk.isEmpty) {
            if(availableSpace < stk.count) player.inventory.offerOrDrop(player.world, stk)
            else screenHandler.network.insertStack(stk)
        }
        screenHandler.slots[it].stack = ItemStack.EMPTY
        player.networkHandler.sendPacket(ScreenHandlerSlotUpdateS2CPacket(screenHandler.syncId, it, ItemStack.EMPTY))
    }
}

private fun updateCursorStack(playerInventory: PlayerInventory, stack: ItemStack) {
    val passedData = PacketByteBuf(Unpooled.buffer())
    passedData.writeItemStack(stack)
    playerInventory.cursorStack = stack
    ServerSidePacketRegistry.INSTANCE.sendToPlayer(playerInventory.player, UPDATE_CURSOR_SLOT_S2C_PACKET, passedData)
}

private fun changeColor(string: String, player: PlayerEntity, network: Network) {
    var color: Color? = null
    val args = string.split("\\s+")
    try {
        if (args.size == 1 && args[0].length == 6) {
            color = Color.decode("#" + args[0].toUpperCase())
        } else if (args.size == 3) {
            color = Color(
                Integer.valueOf(args[0]),
                Integer.valueOf(args[1]),
                Integer.valueOf(args[2])
            )
        }
    } catch (ignored: Exception) {}
    if(color == null) {
        player.sendMessage(LiteralText("Could not parse color!"), false)
    }else{
        val be = network.controller?.let { player.world.getBlockEntity(it) }
        if(be is ControllerBlockEntity) be.storedColor = color
        network.updateColor()
    }
}

private fun executeMouseClicker(stack: ItemStack, playerInventory: PlayerInventory, shift: Boolean, network: Network, button: Int) {
    if (shift) {
        val copyStack = stack.copy()
        playerInventory.insertStack(stack)
        if(!stack.isEmpty) copyStack.decrement(stack.count)
        network.removeStack(copyStack)
        if(!copyStack.isEmpty) {
            playerInventory.main.remove(copyStack)
        }
    }else{
        if (button == 0) {
            if (playerInventory.cursorStack.isEmpty) {
                val copyStack = stack.copy()
                network.removeStack(stack)
                if(stack.isEmpty) updateCursorStack(playerInventory, copyStack)
                else {
                    copyStack.decrement(stack.count)
                    updateCursorStack(playerInventory, copyStack)
                }
            } else {
                val insertStack = network.insertStack(playerInventory.cursorStack)
                updateCursorStack(playerInventory, insertStack)
            }
        }
        if (button == 1) {
            if (playerInventory.cursorStack.isEmpty) {
                stack.decrement(stack.count / 2)
                val copyStack = stack.copy()
                network.removeStack(stack)
                if(stack.isEmpty) updateCursorStack(playerInventory, copyStack)
                else {
                    copyStack.decrement(stack.count)
                    updateCursorStack(playerInventory, copyStack)
                }
            } else {
                val oneStack = playerInventory.cursorStack.copy()
                oneStack.count = 1;
                val resultStack = network.insertStack(oneStack)
                if (resultStack.isEmpty) {
                    playerInventory.cursorStack.decrement(1)
                    val newStack = playerInventory.cursorStack
                    updateCursorStack(playerInventory, newStack)
                }
            }
        }
    }
}

val UPDATE_TERMINAL_CONFIG_S2C_PACKET = Identifier(MOD_ID, "update_terminal_config_s2c")
val UPDATE_COLOR_MAP_S2C_PACKET = Identifier(MOD_ID, "update_color")
val UPDATE_CURSOR_SLOT_S2C_PACKET = Identifier(MOD_ID, "update_cursor_slot")
val SYNCHRONIZE_LAST_RECIPE_PACKET = Identifier(MOD_ID, "synchronize_last_recipe")

val colorMap = mutableMapOf<BlockPos, Color>()
var terminalConfig = TerminalConfig()

fun initNetworkPacketsClient() {
    ClientSidePacketRegistry.INSTANCE.register(UPDATE_TERMINAL_CONFIG_S2C_PACKET) { packetContext: PacketContext, attachedData: PacketByteBuf ->
        packetContext.run {
            attachedData.readCompoundTag()?.let { terminalConfig.fromTag(it) }
        }
    }
    ClientSidePacketRegistry.INSTANCE.register(UPDATE_COLOR_MAP_S2C_PACKET) { packetContext: PacketContext, attachedData: PacketByteBuf ->
        val pos = attachedData.readBlockPos()
        val color = Color(attachedData.readInt())
        packetContext.taskQueue.execute {
            colorMap[pos] = color
            val world = packetContext.player.world
            val state = world.getBlockState(pos)
            MinecraftClient.getInstance().worldRenderer.updateBlock(world, pos, state, state, 0)
        }
    }
    ClientSidePacketRegistry.INSTANCE.register(UPDATE_CURSOR_SLOT_S2C_PACKET) { packetContext: PacketContext, attachedData: PacketByteBuf ->
        val stack = attachedData.readItemStack()
        packetContext.taskQueue.execute {
            packetContext.player.inventory.cursorStack = stack
        }
    }
    ClientSidePacketRegistry.INSTANCE.register(SYNCHRONIZE_LAST_RECIPE_PACKET) { packetContext: PacketContext, attachedData: PacketByteBuf ->
        val type = attachedData.readInt()
        val id = attachedData.readIdentifier()
        val recipe = if(type == 0) RecipeSerializer.SHAPED.read(id, attachedData)
        else RecipeSerializer.SHAPELESS.read(id, attachedData)
        packetContext.taskQueue.execute {
            val screenHandler = packetContext.player.currentScreenHandler
            if(screenHandler is CraftingTerminalScreenHandler) {
                screenHandler.lastRecipe = recipe
            }
        }
    }
}