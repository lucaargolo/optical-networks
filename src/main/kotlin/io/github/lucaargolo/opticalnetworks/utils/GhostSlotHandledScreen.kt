package io.github.lucaargolo.opticalnetworks.utils

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.network.GHOST_SLOT_CLICK_C2S_PACKET
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.item.TooltipContext
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.Text

open class GhostSlotHandledScreen<T: ScreenHandler>(handler: T, inventory: PlayerInventory, title: Text): HandledScreen<T>(handler, inventory, title) {

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
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

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) { }

}