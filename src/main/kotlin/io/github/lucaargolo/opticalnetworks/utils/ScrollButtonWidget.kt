package io.github.lucaargolo.opticalnetworks.utils

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.MOD_ID
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.LiteralText
import net.minecraft.util.Identifier

class ScrollButtonWidget(x: Int, y: Int, action: PressAction): ButtonWidget(x, y, 10, 15, LiteralText(""), action) {

    private val DEFAULT: Identifier = Identifier(MOD_ID, "textures/gui/generic.png")

    var isPressed: Boolean = false;

    override fun renderButton(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        MinecraftClient.getInstance().textureManager.bindTexture(DEFAULT)
        var i = 7
        if (this.isPressed) i += 10
        else if (this.isHovered) i += 20
        drawTexture(matrices, this.x, this.y, i, 25, 10, 15)
    }

}