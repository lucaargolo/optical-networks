package io.github.lucaargolo.opticalnetworks.integration

import io.github.lucaargolo.opticalnetworks.blocks.terminal.BlueprintTerminalScreen
import io.github.lucaargolo.opticalnetworks.blocks.terminal.BlueprintTerminalScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.terminal.CraftingTerminalScreen
import io.github.lucaargolo.opticalnetworks.blocks.terminal.CraftingTerminalScreenHandler
import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.packets.MOVE_BLUEPRINT_TERMINAL_ITEMS_PACKET_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.packets.MOVE_TERMINAL_ITEMS_PACKET_C2S_PACKET
import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import me.shedaniel.rei.RoughlyEnoughItemsNetwork
import me.shedaniel.rei.api.AutoTransferHandler
import me.shedaniel.rei.api.EntryStack
import me.shedaniel.rei.api.TransferRecipeDisplay
import me.shedaniel.rei.plugin.autocrafting.DefaultCategoryHandler
import me.shedaniel.rei.server.ContainerInfo
import me.shedaniel.rei.server.ContainerInfoHandler
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.resource.language.I18n
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.util.collection.DefaultedList

@Suppress("UNCHECKED_CAST")
class CustomCategoryHandler: DefaultCategoryHandler() {

    override fun handle(context: AutoTransferHandler.Context): AutoTransferHandler.Result? {
        if (context.recipe is TransferRecipeDisplay && context.container is CraftingTerminalScreenHandler) {

            val recipe = context.recipe as TransferRecipeDisplay
            val containerScreen = context.containerScreen as CraftingTerminalScreen
            val container = context.container as CraftingTerminalScreenHandler

            val containerInfo = ContainerInfoHandler.getContainerInfo(
                recipe.recipeCategory,
                container.javaClass
            ) as ContainerInfo<ScreenHandler>

            if (recipe.height > containerInfo.getCraftingHeight(container) || recipe.width > containerInfo.getCraftingWidth(container))
                return AutoTransferHandler.Result.createFailed(
                    I18n.translate(
                        "error.rei.transfer.too_small",
                        containerInfo.getCraftingWidth(container),
                        containerInfo.getCraftingHeight(container)
                    )
                )

            val input = recipe.getOrganisedInputEntries(containerInfo, container)
            val missingInInvList = hasItems(input)
            val missingInNetworkList = hasItems(input, container.network)

            if (!missingInNetworkList.isEmpty()) return AutoTransferHandler.Result.createFailed("error.rei.not.enough.materials", missingInNetworkList)

            if (!canUseMovePackets()) return AutoTransferHandler.Result.createFailed("error.rei.not.on.server")
            if (!context.isActuallyCrafting) return AutoTransferHandler.Result.createSuccessful()
            context.minecraft.openScreen(containerScreen)

            val buf = PacketByteBuf(Unpooled.buffer())
            buf.writeIdentifier(recipe.recipeCategory)
            buf.writeBoolean(Screen.hasShiftDown())
            buf.writeInt(input.size)
            for (stacks in input) {
                buf.writeInt(stacks.size)
                for (stack in stacks) {
                    if (stack.itemStack != null) buf.writeItemStack(stack.itemStack) else buf.writeItemStack(
                        ItemStack.EMPTY
                    )
                }
            }

            if(missingInInvList.isEmpty()) ClientSidePacketRegistry.INSTANCE.sendToServer(RoughlyEnoughItemsNetwork.MOVE_ITEMS_PACKET, buf)
            else ClientSidePacketRegistry.INSTANCE.sendToServer(MOVE_TERMINAL_ITEMS_PACKET_C2S_PACKET, buf)

            return AutoTransferHandler.Result.createSuccessful()

        }else if(context.container is BlueprintTerminalScreenHandler) {

            val recipe = context.recipe
            val containerScreen = context.containerScreen as BlueprintTerminalScreen
            val container = context.container as BlueprintTerminalScreenHandler

            val containerInfo = ContainerInfoHandler.getContainerInfo(
                recipe.recipeCategory,
                container.javaClass
            ) as ContainerInfo<ScreenHandler>?

            containerInfo?.let {
                if (recipe is TransferRecipeDisplay && (recipe.height > containerInfo.getCraftingHeight(container) || recipe.width > containerInfo.getCraftingWidth(container)))
                    return AutoTransferHandler.Result.createFailed(I18n.translate("error.rei.transfer.too_small", containerInfo.getCraftingWidth(container), containerInfo.getCraftingHeight(container)))
            }

            val input = if(containerInfo != null && recipe is TransferRecipeDisplay) recipe.getOrganisedInputEntries(containerInfo, container) else recipe.inputEntries
            val output = recipe.outputEntries

            if (!canUseMovePackets()) return AutoTransferHandler.Result.createFailed("error.rei.not.on.server")
            if (!context.isActuallyCrafting) return AutoTransferHandler.Result.createSuccessful()
            context.minecraft.openScreen(containerScreen)

            val buf = PacketByteBuf(Unpooled.buffer())
            buf.writeIdentifier(recipe.recipeCategory)
            buf.writeBoolean(Screen.hasShiftDown())
            buf.writeInt(input.size)
            for (stacks in input) {
                buf.writeInt(stacks.size)
                for (stack in stacks) {
                    if (stack.itemStack != null) buf.writeItemStack(stack.itemStack) else buf.writeItemStack(ItemStack.EMPTY)
                }
            }
            buf.writeInt(output.size)
            for (stack in output) {
                if (stack.itemStack != null) buf.writeItemStack(stack.itemStack) else buf.writeItemStack(ItemStack.EMPTY)
            }

            ClientSidePacketRegistry.INSTANCE.sendToServer(MOVE_BLUEPRINT_TERMINAL_ITEMS_PACKET_C2S_PACKET, buf)
        }
        return AutoTransferHandler.Result.createNotApplicable()
    }

    override fun getPriority(): Double {
        return 10.0
    }

    private fun hasItems(inputs: List<List<EntryStack>>, network: Network): IntList {
        val copyMain = DefaultedList.of<ItemStack>()
        for (stack in MinecraftClient.getInstance().player!!.inventory.main) {
            copyMain.add(stack.copy())
        }
        copyMain.addAll(network.getAvailableStacks(""))

        val intList: IntList = IntArrayList()
        for (i in inputs.indices) {
            val possibleStacks = inputs[i]
            var done = possibleStacks.isEmpty()
            for (possibleStack in possibleStacks) {
                if (!done) {
                    var invRequiredCount = possibleStack.amount
                    for (stack in copyMain) {
                        val entryStack = EntryStack.create(stack)
                        if (entryStack == possibleStack) {
                            while (invRequiredCount > 0 && !stack.isEmpty) {
                                invRequiredCount--
                                stack.decrement(1)
                            }
                        }
                    }
                    if (invRequiredCount <= 0) {
                        done = true
                        break
                    }
                }
            }
            if (!done) {
                intList.add(i)
            }
        }
        return intList
    }
}