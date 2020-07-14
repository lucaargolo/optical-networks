package io.github.lucaargolo.opticalnetworks.utils.widgets

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.LiteralText
import net.minecraft.util.Identifier

class SmallButtonWidget(x: Int, y: Int, private val u: Int, private val v: Int, pressAction: PressAction, val texture: Identifier): ButtonWidget(x, y, 8, 8, LiteralText(""), pressAction ), PressableWidget {

    override var isPressed = false

    override fun renderButton(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        if(!active) return
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        MinecraftClient.getInstance().textureManager.bindTexture(texture)
        if(isPressed) drawTexture(matrices, x, y, u+8, v, 8, 8)
        else drawTexture(matrices, x, y, u, v, 8, 8)
        if(isPressed || isHovered) DrawableHelper.fill(matrices, x + 1, y + 1, x + 7, y + 7, -2130706433)
    }

}