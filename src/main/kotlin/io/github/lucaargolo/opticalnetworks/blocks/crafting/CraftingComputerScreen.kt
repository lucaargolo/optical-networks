package io.github.lucaargolo.opticalnetworks.blocks.crafting

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.utils.widgets.ScrollButtonWidget
import io.github.lucaargolo.opticalnetworks.utils.widgets.SmallButtonWidget
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ButtonWidget.PressAction
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper

class CraftingComputerScreen(handler: CraftingComputerScreenHandler, inventory: PlayerInventory, title: Text): HandledScreen<CraftingComputerScreenHandler>(handler, inventory, title) {

    private val texture = Identifier("opticalnetworks:textures/gui/crafting_computer.png")

    private var scrollPages = 1
    private var scrollOffset = 0
    private var scrollable = false
    private var isScrolling = false
    private var scrollButton: ScrollButtonWidget? = null

    private var increasePriorityButtonList: MutableList<ButtonWidget> = mutableListOf()
    private var decreasePriorityButtonList: MutableList<ButtonWidget> = mutableListOf()

    override fun init() {
        super.init()
        backgroundHeight = 185
        x = width/2-backgroundWidth/2
        y = height/2-backgroundHeight/2
        scrollOffset = y + 18
        scrollButton = ScrollButtonWidget(x + 158, y + 18, PressAction { if(scrollable) isScrolling = true })
        this.addButton(scrollButton)
        increasePriorityButtonList = mutableListOf()
        (0..3).forEach {
            val button = SmallButtonWidget(143, 18+(it*18), 176, 8, PressAction {  }, texture)
            increasePriorityButtonList.add(button)
            this.addButton(button)
        }
        decreasePriorityButtonList = mutableListOf()
        (0..3).forEach {
            val button = SmallButtonWidget(143, 26+(it*18), 176, 16, PressAction { }, texture)
            decreasePriorityButtonList.add(button)
            this.addButton(button)
        }
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)

        val be = handler.entity

        scrollButton?.isPressed = this.isScrolling
        scrollButton?.y = scrollOffset

        val activeSlots = be.getSortedQueue().size
        scrollable = activeSlots > 4
        scrollPages = activeSlots - 4

        val scrollPage = getScrollPage()

        var index = 0
        var indexPage = 0
        (0..3).forEach {
            increasePriorityButtonList[it].active = false
            decreasePriorityButtonList[it].active = false
        }
        be.getSortedQueue().forEach { (action, priority) ->
            if (indexPage >= scrollPage) {
                if (index < 4) {
                    increasePriorityButtonList[index].active = true
                    decreasePriorityButtonList[index].active = true
                    this.itemRenderer.renderInGuiWithOverrides(this.client!!.player, action.outputStacks[0], x + 81, y + 18 + (index * 18))
                    matrices.scale(0.5F, 0.5F, 1.0F)
                    textRenderer.draw(matrices, TranslatableText("tooltip.opticalnetworks.quantity").append(LiteralText(action.quantity.toString())), (x + 100f) * 2, (y + 21f + (index * 18)) * 2, 4210752)
                    textRenderer.draw(matrices, TranslatableText("tooltip.opticalnetworks.enum.state.${action.state.name.toLowerCase()}"), (x + 100f) * 2, (y + 21f + (index * 18) + 5f) * 2, 4210752)
                    matrices.scale(2.0F, 2.0F, 1.0F)
                }
                index++
            } else {
                indexPage++
            }
        }

        drawMouseoverTooltip(matrices, mouseX, mouseY)
    }

    private fun getScrollPage(): Int {
        val scrollPercentage = (scrollOffset-(y+18)).toFloat()/(y+73-(y+18)).toFloat()
        return (scrollPercentage/(1f/ scrollPages)).toInt()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        val scrollPage = getScrollPage()
        if(amount > 0) {
            if(scrollPage > 0) {
                val desiredPercentage = (scrollPage-1f)/scrollPages
                scrollOffset = MathHelper.ceil((desiredPercentage*(y+73-(y+18)) + (y+18)))
            }
        }else{
            if(scrollPage < scrollPages) {
                val desiredPercentage = (scrollPage+1f)/scrollPages
                scrollOffset = MathHelper.ceil(desiredPercentage*(y+73-(y+18)) + (y+18))
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return if (isScrolling) {
            scrollOffset = MathHelper.clamp(mouseY.toInt(), y+18, y+73)
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

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(texture)
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        textRenderer.draw(matrices, title.string, 8f, 6f, 4210752)
        textRenderer.draw(matrices, playerInventory.displayName, 8f, 184 - 96 + 4f, 4210752)
    }
}