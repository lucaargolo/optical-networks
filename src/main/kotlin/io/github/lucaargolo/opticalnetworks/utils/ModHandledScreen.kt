package io.github.lucaargolo.opticalnetworks.utils

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.packets.GHOST_SLOT_CLICK_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.utils.widgets.EnumButtonWidget
import io.github.lucaargolo.opticalnetworks.utils.widgets.GhostSlot
import io.github.lucaargolo.opticalnetworks.utils.widgets.PressableWidget
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.item.TooltipContext
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.LiteralText
import net.minecraft.text.Text

open class ModHandledScreen<T: ScreenHandler>(handler: T, inventory: PlayerInventory, title: Text): HandledScreen<T>(handler, inventory, title) {

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(matrices, mouseX, mouseY, delta)
        if(this.handler is GhostSlot.IScreenHandler) drawGhostSlots(matrices, mouseX, mouseY, delta)

        this.buttons.forEach {
            if(it is EnumButtonWidget<*>) {
                if(it.isHovered) {
                    val aux = zOffset
                    zOffset = 5000
                    val tooltip = mutableListOf<Text>()
                    tooltip.add(LiteralText(it.state::class.simpleName))
                    val macumba = it.state.name.substring(0, 1) + it.state.name.toLowerCase().substring(1, it.state.name.length)
                    tooltip.add(LiteralText("Selected: $macumba"))
                    renderTooltip(matrices, tooltip, mouseX, mouseY)
                    zOffset = aux
                }
            }
        }
    }

    private fun drawGhostSlots(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        (this.handler as GhostSlot.IScreenHandler).ghostSlots.forEach { ghostSlot ->
            val i = x+ghostSlot.x+1
            val j = y+ghostSlot.y+1
            val stack = handler.getGhostInv()[ghostSlot.index]
            RenderSystem.enableDepthTest()
            client!!.itemRenderer.renderInGuiWithOverrides(playerInventory.player, stack, i, j)
            if(ghostSlot.isQuantifiable && stack.count > 1)
                client!!.itemRenderer.renderGuiItemOverlay(textRenderer, stack, i, j, stack.count.toString())
            if(mouseX in (i-1..i+17) && mouseY in (j-1..j+17)) {
                DrawableHelper.fill(matrices, i, j, i + 16, j + 16, -2130706433)
                if(playerInventory.cursorStack?.isEmpty == true && !stack.isEmpty)
                    renderTooltip(matrices, stack.getTooltip(playerInventory.player, TooltipContext.Default.NORMAL), mouseX, mouseY)
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(button in (0..1) && this.handler is GhostSlot.IScreenHandler) {
            this.handler.ghostSlots.forEach {
                val i = x+it.x+1
                val j = y+it.y+1
                if(mouseX.toInt() in (i-1..i+17) && mouseY.toInt() in (j-1..j+17)) {
                    val newStack = playerInventory.cursorStack?.copy()
                    val passedData = PacketByteBuf(Unpooled.buffer())
                    passedData.writeInt(button)
                    passedData.writeBoolean(InputUtil.isKeyPressed(client!!.window.handle, 340) || InputUtil.isKeyPressed(client!!.window.handle, 344))
                    passedData.writeBoolean(it.isQuantifiable)
                    passedData.writeBlockPos(handler.getBlockEntity().pos)
                    passedData.writeItemStack(newStack)
                    passedData.writeInt(it.index)
                    ClientSidePacketRegistry.INSTANCE.sendToServer(GHOST_SLOT_CLICK_C2S_PACKET, passedData)
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        this.buttons.forEach {
            if(it is PressableWidget) it.isPressed = false
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) { }

}