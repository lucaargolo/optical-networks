package io.github.lucaargolo.opticalnetworks.blocks.`interface`

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.blocks.attachment.AttachmentScreen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier

class InterfaceScreen(handler: InterfaceScreenHandler, inventory: PlayerInventory, title: Text): AttachmentScreen(handler, inventory, title) {

    private val texture = Identifier("opticalnetworks:textures/gui/interface.png")

    override fun updateSize() {
        backgroundHeight = 220
        y = height/2-backgroundHeight/2
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(texture)
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        textRenderer.draw(matrices, title.string, (backgroundWidth/2 - textRenderer.getWidth(title.string) / 2f), 6f, 4210752)
        val importerText = TranslatableText("screen.opticalnetworks.importer")
        textRenderer.draw(matrices, importerText, 25+(backgroundWidth-(backgroundWidth-54))/2 - textRenderer.getWidth(importerText) / 2f, 24f, 4210752)
        val exporterText = TranslatableText("screen.opticalnetworks.exporter")
        textRenderer.draw(matrices, exporterText, 97+(backgroundWidth-(backgroundWidth-54))/2 - textRenderer.getWidth(exporterText) / 2f, 24f, 4210752)
        textRenderer.draw(matrices, LiteralText("Blueprints"), 8f, 74+23f, 4210752)
        textRenderer.draw(matrices, playerInventory.displayName, 8f, 74+54f, 4210752)
    }

}