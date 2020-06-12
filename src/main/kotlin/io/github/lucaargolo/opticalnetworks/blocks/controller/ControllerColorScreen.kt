package io.github.lucaargolo.opticalnetworks.blocks.controller

import io.github.lucaargolo.opticalnetworks.network.NETWORK_INTERACT
import io.github.lucaargolo.opticalnetworks.network.NetworkState
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.LiteralText

class ControllerColorScreen(val network: NetworkState.Network): Screen(LiteralText("Color Selection")) {

    private var colorBox: TextFieldWidget? = null
    private var setButton: ButtonWidget? = null

    override fun init() {
        super.init()
        colorBox = TextFieldWidget(textRenderer, (width/2)-30, (height/2)-5, 60, 10, LiteralText(""))
        colorBox!!.setMaxLength(12)
        colorBox!!.setEditableColor(16777215)
        this.addChild(colorBox)
        setButton = ButtonWidget((width/2)-30, (height/2)+10, 60, 20, LiteralText("Set Color"), ButtonWidget.PressAction {
            val passedData = PacketByteBuf(Unpooled.buffer())
            passedData.writeUuid(network.id)
            passedData.writeInt(2)
            passedData.writeString(colorBox?.text)
            ClientSidePacketRegistry.INSTANCE.sendToServer(NETWORK_INTERACT, passedData)
            onClose()
        })
        this.addButton(setButton)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return if (colorBox!!.keyPressed(keyCode, scanCode, modifiers)) true
        else if (colorBox!!.isFocused && colorBox!!.isVisible && keyCode != 256) true
        else super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        val text = LiteralText("Please write the color code: ")
        textRenderer.draw(matrices, text, (width/2f)-(textRenderer.getWidth(text)/2f), (height/2f)-20, 0xFFFFFF)
        colorBox?.render(matrices, mouseX, mouseY, delta)
    }

    override fun isPauseScreen() = false
}