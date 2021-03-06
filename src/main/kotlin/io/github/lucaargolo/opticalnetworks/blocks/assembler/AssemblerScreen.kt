package io.github.lucaargolo.opticalnetworks.blocks.assembler

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.math.min

class AssemblerScreen(handler: AssemblerScreenHandler, inventory: PlayerInventory, title: Text): HandledScreen<AssemblerScreenHandler>(handler, inventory, title) {

    private val texture = Identifier("opticalnetworks:textures/gui/assembler.png")

    override fun init() {
        super.init()
        x = width / 2 - backgroundWidth / 2
        y = height / 2 - backgroundHeight / 2
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        drawMouseoverTooltip(matrices, mouseX, mouseY)
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(texture)
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight)
        val progress = min(handler.entity.processingTime/AssemblerBlockEntity.PROCESSING_GOAL.toFloat(), 1f)
        drawTexture(matrices, x+114, y+35, 176, 0, (22*progress).toInt(), 16)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        textRenderer.draw(matrices, title.string, 53+(54/2 - textRenderer.getWidth(title.string)/2f), 6f, 4210752)
        textRenderer.draw(matrices, playerInventory.displayName, 8f, 166 - 94f, 4210752)
    }

}