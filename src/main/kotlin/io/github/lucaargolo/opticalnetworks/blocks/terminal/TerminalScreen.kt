package io.github.lucaargolo.opticalnetworks.blocks.terminal

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.network.NETWORK_INTERACT
import io.github.lucaargolo.opticalnetworks.utils.EnumButtonWidget
import io.github.lucaargolo.opticalnetworks.utils.ScrollButtonWidget
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper

class TerminalScreen(handler: TerminalScreenHandler, inventory: PlayerInventory, title: Text): HandledScreen<TerminalScreenHandler>(handler, inventory, title) {

    enum class Size(val row: Int, val texture: Identifier?, val x: Int, val y: Int) {
        SMALL(5, Identifier("opticalnetworks:textures/gui/terminal_small.png"), 193, 203),
        MEDIUM(7, Identifier("opticalnetworks:textures/gui/terminal_normal.png"), 193, 239);
        //LARGE(15, Identifier("opticalnetworks:textures/gui/terminal_tall.png"), 193, 300)

        val automatic: Boolean = (texture != null)
    }

    private var size = Size.MEDIUM
    private var scrollPages = 1
    private var scrollOffset = 0
    private var scrollable = false
    private var isScrolling = false
    private var activeSlots = 0
    private var scrollButton: ScrollButtonWidget? = null

    private var sizeButton: EnumButtonWidget<Size>? = null

    private var searchBox: TextFieldWidget? = null

    private var bWidth = 0
    private var bHeight = 0

    private var hoverTerminalSlot: TerminalSlot? = null

    override fun tick() {
        searchBox?.tick()
    }

    override fun init() {
        super.init()
        bWidth = size.x
        bHeight = size.y
        x = (width - bWidth) / 2
        y = (height - bHeight) / 2

        searchBox = TextFieldWidget(textRenderer, x+81, y+5, 80, 9, TranslatableText("itemGroup.search"))
        searchBox!!.setMaxLength(50)
        searchBox!!.setHasBorder(false)
        searchBox!!.setEditableColor(16777215)
        this.addChild(searchBox)

        sizeButton = EnumButtonWidget(x-18, y + 4, ButtonWidget.PressAction {sizeButton?.change()}, size, Size.values(), Size.MEDIUM.texture!!, 193, 0)
        this.addButton(sizeButton)

        scrollOffset = y + 18
        scrollButton = ScrollButtonWidget(x+175, y + 18, ButtonWidget.PressAction{})
        this.addButton(scrollButton)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return if (searchBox!!.keyPressed(keyCode, scanCode, modifiers)) true
        else if (searchBox!!.isFocused && searchBox!!.isVisible && keyCode != 256) true
        else super.keyPressed(keyCode, scanCode, modifiers)
    }


    override fun resize(client: MinecraftClient?, width: Int, height: Int) {
        val string = searchBox!!.text
        this.init(client, width, height)
        searchBox!!.text = string
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        if (sizeButton != null && sizeButton!!.state != size) {
            size = sizeButton!!.state
            init(client, width, height)
            if(handler.terminalSlots.size > size.row*9) {
                val it = handler.terminalSlots.iterator()
                var index = 0
                while(it.hasNext()) {
                    it.next()
                    if(index >= size.row*9) {
                        it.remove()
                    }
                    index++
                }
            }
            if(handler.terminalSlots.size < size.row*9) {
                val i = 18 + 18 * handler.terminalSlots.size/9
                    (0 until size.row*9-handler.terminalSlots.size).forEach {
                    var m = it
                    var n = 0
                    while(m/9 > 0) {
                        n++
                        m -= 9
                    }
                    handler.terminalSlots.add(TerminalSlot(8 + m * 18, n * 18 + i))
                }
            }
        }

        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)

        searchBox?.render(matrices, mouseX, mouseY, delta)

        scrollButton?.isPressed = this.isScrolling
        scrollButton?.y = scrollOffset

        hoverTerminalSlot = null
        handler.terminalSlots.forEach {slot ->
            val i = slot.x + x - 1
            val j = slot.y + y - 1
            if(mouseX in (i..i+18) && mouseY in (j..j+18)) hoverTerminalSlot = slot
            slot.item = ItemStack.EMPTY
            slot.count = 0
        }

        val allStacks = handler.network.searchStacks(searchBox?.text ?: "")

        activeSlots = allStacks.size
        scrollable = activeSlots/9f > (size.row).toFloat()
        scrollPages = (MathHelper.ceil(activeSlots/9f) - size.row)

        val scrollPage = getScrollPage()

        var index = 0
        var indexPage = 0
        allStacks.forEach {
            if(indexPage >= 9*scrollPage) {
                val slot = handler.terminalSlots.getOrNull(index)
                if(slot != null) {
                    slot.item = it
                    slot.count = it.count

                    drawTerminalSlot(matrices, slot, mouseX, mouseY)
                }
                index++
            }else{
                indexPage++
            }
        }

        drawMouseoverTooltip(matrices, mouseX, mouseY)

        this.buttons.forEach {
            if(it is EnumButtonWidget<*>) {
                if(it.isHovered()) {
                    val tooltip = mutableListOf<Text>()
                    tooltip.add(LiteralText(it.state::class.simpleName))
                    val macumba = it.state.name.substring(0, 1) + it.state.name.toLowerCase().substring(1, it.state.name.length)
                    tooltip.add(LiteralText("Selected: $macumba"))
                    renderTooltip(matrices, tooltip, mouseX, mouseY)
                }
            }
        }
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return if (isScrolling) {
            scrollOffset = MathHelper.clamp(mouseY.toInt(), y+18, y+(size.row*18)+1)
            true
        } else {
            super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        }
    }

    private fun getScrollPage(): Int {
        val scrollPercentage = (scrollOffset-(y+18)).toFloat()/(y+(size.row*18)+1-(y+18)).toFloat()
        return (scrollPercentage/(1f/ scrollPages)).toInt()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        val scrollPage = getScrollPage()
        if(amount > 0) {
            if(scrollPage > 0) {
                val desiredPercentage = (scrollPage-1f)/scrollPages
                scrollOffset = MathHelper.ceil((desiredPercentage*(y+(size.row*18)+1-(y+18)) + (y+18)))
            }
        }else{
            if(scrollPage < scrollPages) {
                val desiredPercentage = (scrollPage+1f)/scrollPages
                scrollOffset = MathHelper.ceil(desiredPercentage*(y+(size.row*18)+1-(y+18)) + (y+18))
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isScrolling = false
        isScrolling = if(scrollButton != null) (scrollable && mouseX > scrollButton!!.x && mouseX < scrollButton!!.x+10 && mouseY > scrollButton!!.y && mouseY <= scrollButton!!.y+15) else isScrolling
        if(hoverTerminalSlot != null && button in (0..1)) {
            val passedData = PacketByteBuf(Unpooled.buffer())
            passedData.writeUuid(handler.network.id)
            passedData.writeInt(1)
            passedData.writeInt(button)
            passedData.writeBoolean(InputUtil.isKeyPressed(client!!.window.handle, 340) || InputUtil.isKeyPressed(client!!.window.handle, 344))
            val placeholderStack = hoverTerminalSlot!!.item.copy()
            if (placeholderStack.count > placeholderStack.maxCount) placeholderStack.count = placeholderStack.maxCount
            passedData.writeItemStack(placeholderStack)
            ClientSidePacketRegistry.INSTANCE.sendToServer(NETWORK_INTERACT, passedData)
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        this.buttons.forEach {
            if(it is EnumButtonWidget<*>) it.isPressed = false
        }
        isScrolling = false
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun isClickOutsideBounds(mouseX: Double, mouseY: Double, left: Int, top: Int, button: Int): Boolean {
        return mouseX !in (x..x+bWidth) && mouseY !in (y..y+bHeight)
    }

    private fun drawTerminalSlot(matrices: MatrixStack, slot: TerminalSlot, mouseX: Int, mouseY: Int) {
        val i = slot.x + x
        val j = slot.y + y

        this.zOffset = 100
        this.itemRenderer.zOffset = 100.0f

        RenderSystem.enableDepthTest()
        this.itemRenderer.method_27951(this.client!!.player, slot.item, i, j)

        val string = slot.getCountString()
        val matrixStack = MatrixStack()
        val w = i*2f + 16f
        val p = j*2f + 16f
        matrixStack.translate(0.0, 0.0, (zOffset + 200.0))
        matrixStack.scale(0.5f, 0.5f, 1.0f)
        val immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().buffer)
        textRenderer.draw(string, (w + 17f - textRenderer.getWidth(string)), p + 9f, 16777215, true, matrixStack.peek().model, immediate, false, 0, 15728880)
        immediate.draw()

        this.itemRenderer.zOffset = 0.0f
        this.zOffset = 0

        if(hoverTerminalSlot == slot) {
            DrawableHelper.fill(matrices, i, j, i + 16, j + 16, -2130706433)
            if(playerInventory.cursorStack.isEmpty) renderTooltip(matrices, slot.item, mouseX, mouseY)
        }
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(size.texture)
        drawTexture(matrices, x, y, 0, 0, bWidth, bHeight)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        val i = (size.row-5)
        textRenderer.draw(matrices, title.string, 6f, 6f, 4210752)
        textRenderer.draw(matrices, playerInventory.displayName, 6f,  110f+(i*18), 4210752)
    }


}