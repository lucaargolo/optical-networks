package io.github.lucaargolo.opticalnetworks.blocks.controller

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.utils.ModHandledScreen
import io.github.lucaargolo.opticalnetworks.utils.widgets.PressableWidget
import io.github.lucaargolo.opticalnetworks.utils.widgets.ScrollButtonWidget
import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper

class ControllerScreen(handler: ControllerScreenHandler, inventory: PlayerInventory, title: Text): ModHandledScreen<ControllerScreenHandler>(handler, inventory, title) {

    private val generic = Identifier("opticalnetworks:textures/gui/generic.png")
    private val texture = Identifier("opticalnetworks:textures/gui/controller.png")

    private val bWidth = 176
    private val bHeight = 203

    private var scrollPages = 1
    private var scrollOffset = 0
    private var scrollable = false
    private var isScrolling = false
    private var scrollButton: ScrollButtonWidget? = null

    private var colorButton: ButtonWidget? = null

    override fun isPauseScreen() = false

    override fun init() {
        super.init()
        x = width/2-bWidth/2
        y = height/2-bHeight/2

        colorButton = object: ButtonWidget(x-18, y+4, 16, 16, LiteralText(""), PressAction { client!!.openScreen(ControllerColorScreen(handler.network)) }),
            PressableWidget {
            override var isPressed: Boolean = false;

            override fun renderButton(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
                RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
                MinecraftClient.getInstance().textureManager.bindTexture(texture)
                if(isPressed) drawTexture(matrices, this.x, this.y, 192, 0, 16, 16)
                else drawTexture(matrices, this.x, this.y, 176, 0, 16, 16)
                if(isPressed || isHovered) DrawableHelper.fill(matrices, this.x + 1, this.y + 1, this.x + 1 + 14, this.y + 1 + 14, -2130706433)
            }
        }
        this.addButton(colorButton)

        scrollOffset = y + 18
        scrollButton = ScrollButtonWidget(x + 158, y + 18, ButtonWidget.PressAction { if(scrollable) isScrolling = true})
        this.addButton(scrollButton)
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
        scrollButton?.isPressed = this.isScrolling
        scrollButton?.y = scrollOffset

        val blockMap = mutableMapOf<Block, Int>()
        handler.network.componentsMap.forEach { (_, block) ->
            if(blockMap[block] == null) blockMap[block] = 1
            else blockMap[block] = blockMap[block]!! + 1
        }
        handler.network.componentNetworks.forEach {
            handler.network.state.networks[it]?.componentsMap?.forEach { (_, block) ->
                if(blockMap[block] == null) blockMap[block] = 1
                else blockMap[block] = blockMap[block]!! + 1
            }
        }

        val activeSlots = blockMap.keys.size
        scrollable = activeSlots > 5f
        scrollPages = activeSlots - 5

        val scrollPage = getScrollPage()

        var index = 0
        var indexPage = 0
        blockMap.keys.sortedBy { blockMap[it] }.reversed().forEach { block ->
            if(indexPage >= scrollPage) {
                if(index < 5) {
                    this.itemRenderer.renderInGuiWithOverrides(this.client!!.player, block.asItem().stackForRender, x+45, y+18+(index*18))
                    textRenderer.draw(matrices, TranslatableText(block.translationKey), x+64f, y+22f+(index*18), 4210752)
                    textRenderer.draw(matrices, LiteralText(blockMap[block].toString()), x+142f, y+22f+(index*18), 4210752)
                }
                index++
            }else{
                indexPage++
            }
        }

        if(mouseY in (y+18..y+83)) when(mouseX) {
            in (x+10..x+22) -> drawEnergyTooltip(matrices, mouseX, mouseY)
            in (x+28..x+40) -> drawSpaceTooltip(matrices, mouseX, mouseY)
        }
    }

    override fun isClickOutsideBounds(mouseX: Double, mouseY: Double, left: Int, top: Int, button: Int): Boolean {
        return mouseX !in (x..x+bWidth) && mouseY !in (y..y+bHeight)
    }

    private fun getScrollPage(): Int {
        val scrollPercentage = (scrollOffset-(y+18)).toFloat()/(y+92-(y+18)).toFloat()
        return (scrollPercentage/(1f/ scrollPages)).toInt()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        val scrollPage = getScrollPage()
        if(amount > 0) {
            if(scrollPage > 0) {
                val desiredPercentage = (scrollPage-1f)/scrollPages
                scrollOffset = MathHelper.ceil((desiredPercentage*(y+92-(y+18)) + (y+18)))
            }
        }else{
            if(scrollPage < scrollPages) {
                val desiredPercentage = (scrollPage+1f)/scrollPages
                scrollOffset = MathHelper.ceil(desiredPercentage*(y+92-(y+18)) + (y+18))
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return if (isScrolling) {
            scrollOffset = MathHelper.clamp(mouseY.toInt(), y+18, y+92)
            true
        } else {
            super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isScrolling = false
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isScrolling = false
        return super.mouseReleased(mouseX, mouseY, button)
    }

    private fun drawSpaceTooltip(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        val pair = handler.network.getSpace()
        val texts = mutableListOf<Text>()
        texts.add(LiteralText("${Formatting.BLUE}Storage:"))
        texts.add(LiteralText("${pair.first} / ${pair.second}"))
        renderTooltip(matrices, texts, mouseX, mouseY)
    }

    private fun drawEnergyTooltip(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        val be = handler.network.getController()?.let {
            playerInventory.player.world.getBlockEntity(it)
        }
        if(be is ControllerBlockEntity) {
            val texts = mutableListOf<Text>()
            texts.add(LiteralText("${Formatting.GOLD}Energy:"))
            texts.add(LiteralText("${be.networkStoredPowerCache} / ${handler.network.getMaxStoredPower()}"))
            renderTooltip(matrices, texts, mouseX, mouseY)
        }
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(texture)
        drawTexture(matrices, x, y, 0, 0, bWidth, bHeight)
        client!!.textureManager.bindTexture(generic)
        val be = handler.network.getController()?.let {
            playerInventory.player.world.getBlockEntity(it)
        }
        if(be is ControllerBlockEntity) {
            val energy = MathHelper.ceil((be.networkStoredPowerCache/handler.network.getMaxStoredPower())*65)
            drawTexture(matrices, x+10, y+18+(65-energy), 65, (65-energy), 12, energy)
        }
        val pair = handler.network.getSpace()
        val space = MathHelper.ceil((pair.first.toFloat()/pair.second.toFloat())*65)
        drawTexture(matrices, x+28, y+18+(65-space), 78, (65-space), 12, space)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        textRenderer.draw(matrices, title.string, (bWidth/2 - textRenderer.getWidth(title.string) / 2f), 6f, 4210752)
    }

}