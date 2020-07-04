package io.github.lucaargolo.opticalnetworks.utils.autocrafting

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.blocks.terminal.TerminalScreen
import io.github.lucaargolo.opticalnetworks.packets.REQUEST_CRAFTING_C2S_PACKET
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.LiteralText
import net.minecraft.util.Identifier

class RequestCraftScreen(val terminal: TerminalScreen, val stack: ItemStack): Screen(LiteralText("Crafting")) {

    private val texture = Identifier("opticalnetworks:textures/gui/request_crafting.png")

    private var quantity = 1
        set(value) {
            field = if(value < 1) 1 else if(value > 10000) 10000 else value
        }
    private var quantityBox: TextFieldWidget? = null
    private var plusButton: ButtonWidget? = null
    private var minusButton: ButtonWidget? = null
    private var cancelButton: ButtonWidget? = null
    private var craftButton: ButtonWidget? = null

    private val backgroundWidth = 136
    private val backgroundHeight = 70

    var x = (width - backgroundWidth) / 2
    var y = (height - backgroundHeight) / 2

    private fun isShiftPressed(): Boolean {
        return InputUtil.isKeyPressed(client!!.window.handle, 340) || InputUtil.isKeyPressed(client!!.window.handle, 344)
    }

    override fun init() {
        super.init()
        x = (width - backgroundWidth) / 2
        y = (height - backgroundHeight) / 2
        quantityBox = TextFieldWidget(textRenderer, x+36, y+25, 36, 12, LiteralText(""))
        quantityBox!!.setMaxLength(5)
        quantityBox!!.setHasBorder(false)
        quantityBox!!.setEditableColor(16777215)
        this.addChild(quantityBox)
        minusButton = ButtonWidget(x+8, y+19, 20,  20, LiteralText("-"), ButtonWidget.PressAction {
            if(isShiftPressed()) quantity -= 10
            else quantity--
            quantityBox?.text = quantity.toString()
        })
        this.addButton(minusButton)
        plusButton = ButtonWidget(x+76, y+19, 20, 20, LiteralText("+"), ButtonWidget.PressAction {
            if(isShiftPressed()) quantity += 10
            else quantity++
            quantityBox?.text = quantity.toString()
        })
        this.addButton(plusButton)
        cancelButton = ButtonWidget(x+6, y+44, 60, 20, LiteralText("Cancel"), ButtonWidget.PressAction { onClose() })
        this.addButton(cancelButton)
        craftButton = ButtonWidget(x+70, y+44, 60, 20, LiteralText("Craft"), ButtonWidget.PressAction {
            val passedData = PacketByteBuf(Unpooled.buffer())
            passedData.writeUuid(terminal.hdl.network.id)
            passedData.writeItemStack(stack)
            passedData.writeInt(quantity)
            ClientSidePacketRegistry.INSTANCE.sendToServer(REQUEST_CRAFTING_C2S_PACKET, passedData)
        })
        this.addButton(craftButton)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (quantityBox!!.keyPressed(keyCode, scanCode, modifiers)) return true
        else if (quantityBox!!.isFocused && quantityBox!!.isVisible) return true
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        this.drawBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        quantityBox?.let { if(it.text.isNotEmpty()) quantity = try { Integer.valueOf(quantityBox?.text) } catch(ignored: Exception) { 1 } }
        if(quantityBox?.isFocused == false) quantityBox?.text = quantity.toString()
        quantityBox?.render(matrices, mouseX, mouseY, delta)
        this.drawForeground(matrices)

        RenderSystem.enableDepthTest()
        this.itemRenderer.renderInGuiWithOverrides(this.client!!.player, stack, x+107, y+21)

        if(mouseX in (x+107..x+123) && mouseY in (y+21..y+37)) {
            DrawableHelper.fill(matrices, x+107, y+21, x+123, y+37, -2130706433)
            renderTooltip(matrices, stack, mouseX, mouseY)
        }
    }

    private fun drawBackground(matrices: MatrixStack?) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(texture)
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight)
    }

    private fun drawForeground(matrices: MatrixStack?) {
        val text = LiteralText("Crafting")
        textRenderer.draw(matrices, text, x+(backgroundWidth/2)-(textRenderer.getWidth(text)/2f), y+6f, 4210752)
    }

    override fun onClose() {
        client!!.openScreen(terminal)
    }

    override fun isPauseScreen() = false

}