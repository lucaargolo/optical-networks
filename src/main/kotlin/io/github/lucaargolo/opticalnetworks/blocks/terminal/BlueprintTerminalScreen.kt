package io.github.lucaargolo.opticalnetworks.blocks.terminal

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.mixin.SlotMixin
import io.github.lucaargolo.opticalnetworks.network.GHOST_SLOT_CLICK_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.network.terminalConfig
import io.github.lucaargolo.opticalnetworks.utils.GhostSlot
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.item.TooltipContext
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.Text
import net.minecraft.util.Identifier

@Suppress("DuplicatedCode")
class BlueprintTerminalScreen(handler: BlueprintTerminalScreenHandler, inventory: PlayerInventory, title: Text): CraftingTerminalScreen(handler, inventory, title) {

    override var texture2 = Identifier("opticalnetworks:textures/gui/blueprint_terminal.png")

    override fun repositionSlots() {
        val slotIterator = handler.slots.iterator()
        while(slotIterator.hasNext()) {
            val slot = slotIterator.next()
            when(slot.id) {
                0 -> (slot as SlotMixin).setY(94 + 18*(terminalConfig.size.rows-3))
                in (1..27) -> (slot as SlotMixin).setY(103 + ((slot.id-1)/9) * 18 + 18*(terminalConfig.size.rows-4)+58)
                in (28..36) -> (slot as SlotMixin).setY(161 + 18*(terminalConfig.size.rows-4)+58)
                37 -> (slot as SlotMixin).setY(76 + 18*(terminalConfig.size.rows-3))
                else -> (slot as SlotMixin).setY(76 + 18*(terminalConfig.size.rows-1))
            }
        }
        val ghostSlotIterator = (this.handler as GhostSlot.GhostSlotScreenHandler).ghostSlots.iterator()
        while(ghostSlotIterator.hasNext()) {
            val ghostSlot = ghostSlotIterator.next()
            ghostSlot.y = 75 + (ghostSlot.index/3) * 18 + 18*(terminalConfig.size.rows-3)
        }
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(matrices, mouseX, mouseY, delta)
        drawGhostSlots(matrices, mouseX, mouseY, delta)
    }

    private fun drawGhostSlots(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        (this.handler as GhostSlot.GhostSlotScreenHandler).ghostSlots.forEach {
            val i = x+it.x+1
            val j = y+it.y+1
            val stack = handler.getGhostInv().get(it.index)
            if(mouseX in (i-1..i+17) && mouseY in (j-1..j+17)) DrawableHelper.fill(matrices, i, j, i + 16, j + 16, -2130706433)
            stack.let {
                RenderSystem.enableDepthTest()
                client!!.itemRenderer.renderInGuiWithOverrides(playerInventory.player, stack, i, j)
                if(mouseX in (i-1..i+17) && mouseY in (j-1..j+17) && playerInventory.cursorStack?.isEmpty == true && !stack.isEmpty) {
                    renderTooltip(matrices, stack.getTooltip(playerInventory.player, TooltipContext.Default.NORMAL), mouseX, mouseY)
                }
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(button in (0..1)) {
            (this.handler as GhostSlot.GhostSlotScreenHandler).ghostSlots.forEach {
                val i = x+it.x+1
                val j = y+it.y+1
                if(mouseX.toInt() in (i-1..i+17) && mouseY.toInt() in (j-1..j+17)) {
                    if(button == 0) {
                        val newStack = playerInventory.cursorStack?.copy()
                        if (newStack != null && !newStack.isEmpty) {
                            val passedData = PacketByteBuf(Unpooled.buffer())
                            passedData.writeBlockPos(handler.getBlockEntity().pos)
                            passedData.writeItemStack(newStack)
                            passedData.writeInt(it.index)
                            ClientSidePacketRegistry.INSTANCE.sendToServer(GHOST_SLOT_CLICK_C2S_PACKET, passedData)
                        }
                    }
                    if(button == 1) {
                        val passedData = PacketByteBuf(Unpooled.buffer())
                        passedData.writeBlockPos(handler.getBlockEntity().pos)
                        passedData.writeItemStack(ItemStack.EMPTY)
                        passedData.writeInt(it.index)
                        ClientSidePacketRegistry.INSTANCE.sendToServer(GHOST_SLOT_CLICK_C2S_PACKET, passedData)
                    }

                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }


}