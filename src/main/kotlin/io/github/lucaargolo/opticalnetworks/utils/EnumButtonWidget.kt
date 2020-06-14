package io.github.lucaargolo.opticalnetworks.utils

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.MOD_ID
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class EnumButtonWidget<T: Enum<T>>(x: Int, y: Int, action: PressAction, selectedEnum: T, private val array: Array<T>, private val texture: Identifier, private val u: Int, private val v: Int): ButtonWidget(x, y, 16, 16, LiteralText(""), action ) {

    private val genericTexture: Identifier = Identifier(MOD_ID, "textures/gui/generic.png")

    private var selected = 0

    init {
        array.forEachIndexed { index, enum ->
            if(selectedEnum == enum) selected = index
        }
    }

    fun change() {
        isPressed = true
        if(selected == array.size-1) selected = 0;
        else selected++
    }

    val state: T
    get() {
        return array[selected]
    }

    var isPressed: Boolean = false;

    override fun renderButton(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        MinecraftClient.getInstance().textureManager.bindTexture(genericTexture)
        if(isPressed) drawTexture(matrices, x, y, 16, 64, 16, 16)
        else drawTexture(matrices, x, y, 0, 64, 16, 16)
        if(isPressed || isHovered) DrawableHelper.fill(matrices, x + 1, y + 1, x + 1 + 14, y + 1 + 14, -2130706433)
        MinecraftClient.getInstance().textureManager.bindTexture(texture)
        drawTexture(matrices, x, y, u + selected*16, v, 16, 16)
    }

}
