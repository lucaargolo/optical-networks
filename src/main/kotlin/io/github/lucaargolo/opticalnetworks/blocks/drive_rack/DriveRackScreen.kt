package io.github.lucaargolo.opticalnetworks.blocks.drive_rack

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class DriveRackScreen(handler: DriveRackScreenHandler, inventory: PlayerInventory, title: Text): HandledScreen<DriveRackScreenHandler>(handler, inventory, title) {

    private val texture = Identifier("opticalnetworks:textures/gui/drive_rack.png")

    private var startX = 0
    private var startY = 0

    override fun init() {
        super.init()
        backgroundHeight = 203
        startX = width/2-backgroundWidth/2
        startY = height/2-backgroundHeight/2
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        drawMouseoverTooltip(matrices, mouseX, mouseY)
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(texture)
        drawTexture(matrices, startX, startY, 0, 0, backgroundWidth, backgroundHeight)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        textRenderer.draw(matrices, title.string, (backgroundWidth/2 - textRenderer.getWidth(title.string) / 2f), -12f, 4210752)
        textRenderer.draw(matrices, playerInventory.displayName, 8f, 184 - 96 + 4f, 4210752)
    }
}