package io.github.lucaargolo.opticalnetworks.blocks.terminal

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.mixin.SlotMixin
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
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.registry.Registry

class TerminalScreen(handler: TerminalScreenHandler, inventory: PlayerInventory, title: Text): HandledScreen<TerminalScreenHandler>(handler, inventory, title) {

    enum class Size(var rows: Int, val texture: Identifier?, var x: Int, var y: Int) {
        SHORT(3, Identifier("opticalnetworks:textures/gui/terminal_short.png"), 193, 167),
        REGULAR(5, Identifier("opticalnetworks:textures/gui/terminal_normal.png"), 193, 203),
        TALL(7, Identifier("opticalnetworks:textures/gui/terminal_tall.png"), 193, 239),
        AUTOMATIC(0, Identifier("opticalnetworks:textures/gui/terminal_tall.png"), 193, 0)
    }

    enum class Sort {
        NAME,
        QUANTITY,
        ID
        //INVTWEAKS
    }

    enum class SortDirection {
        ASCENDING,
        DESCENDING
    }

    private var size = Size.TALL
    private var scrollPages = 1
    private var scrollOffset = 0
    private var scrollable = false
    private var isScrolling = false
    private var activeSlots = 0
    private var scrollButton: ScrollButtonWidget? = null

    private var sizeButton: EnumButtonWidget<Size>? = null
    private var sortButton: EnumButtonWidget<Sort>? = null
    private var sortDirectionButton: EnumButtonWidget<SortDirection>? = null

    private var searchBox: TextFieldWidget? = null

    private var bWidth = 0
    private var bHeight = 0

    private var hoverTerminalSlot: TerminalSlot? = null

    override fun tick() {
        searchBox?.tick()
    }

    override fun init() {
        super.init()

        if(size == Size.AUTOMATIC) {
            size.rows = (height-144)/18
            if(size.rows < 3) size.rows = 3
            size.y = 113+(18*size.rows)
        }

        bWidth = size.x
        bHeight = size.y
        x = (width - bWidth) / 2
        y = (height - bHeight) / 2

        val slotIterator = handler.slots.iterator()
        while(slotIterator.hasNext()) {
            val slot = slotIterator.next()
            when(slot.id) {
                in (0..26) -> (slot as SlotMixin).setY(103 + (slot.id/9) * 18 + 18*(size.rows-4))
                else -> (slot as SlotMixin).setY(161 + 18*(size.rows-4))
            }
        }

        if(handler.terminalSlots.size > size.rows*9) {
            val it = handler.terminalSlots.iterator()
            var index = 0
            while(it.hasNext()) {
                it.next()
                if(index >= size.rows*9) it.remove()
                index++
            }
        }

        if(handler.terminalSlots.size < size.rows*9) {
            val i = 18 + 18 * handler.terminalSlots.size/9
            (0 until size.rows*9-handler.terminalSlots.size).forEach {
                handler.terminalSlots.add(TerminalSlot(8 + (it-((it/9)*9))*18, (it/9)*18 + i))
            }
        }

        searchBox = TextFieldWidget(textRenderer, x+81, y+5, 80, 9, LiteralText(""))
        searchBox!!.setMaxLength(50)
        searchBox!!.setHasBorder(false)
        searchBox!!.setEditableColor(16777215)
        this.addChild(searchBox)

        sizeButton = EnumButtonWidget(x-18, y + 4, ButtonWidget.PressAction {
            sizeButton?.change()
            if(sizeButton != null && sizeButton!!.state != size) size = sizeButton!!.state
            init(client, width, height)
        }, size, Size.values(), Size.TALL.texture!!, 193, 0)
        this.addButton(sizeButton)

        sortButton = EnumButtonWidget(x-18, y + 22, ButtonWidget.PressAction {sortButton?.change()}, Sort.NAME, Sort.values(), Size.TALL.texture, 193, 16)
        this.addButton(sortButton)

        sortDirectionButton = EnumButtonWidget(x-18, y + 40, ButtonWidget.PressAction {sortDirectionButton?.change()}, SortDirection.ASCENDING, SortDirection.values(), Size.TALL.texture, 193, 32)
        this.addButton(sortDirectionButton)

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

        var sortedStacks = when(sortButton?.state) {
            Sort.NAME -> allStacks.sortedBy { TranslatableText(it.translationKey).string }
            Sort.QUANTITY -> allStacks.sortedBy { it.count }.reversed()
            Sort.ID -> allStacks.sortedBy { Registry.ITEM.getRawId(it.item) }
            else -> allStacks
        }

        if(sortDirectionButton?.state == SortDirection.DESCENDING) sortedStacks = sortedStacks.reversed()

        activeSlots = sortedStacks.size
        scrollable = activeSlots/9f > (size.rows).toFloat()
        scrollPages = (MathHelper.ceil(activeSlots/9f) - size.rows)

        val scrollPage = getScrollPage()

        var index = 0
        var indexPage = 0
        sortedStacks.forEach {
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
            scrollOffset = MathHelper.clamp(mouseY.toInt(), y+18, y+(size.rows*18)+1)
            true
        } else {
            super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        }
    }

    private fun getScrollPage(): Int {
        val scrollPercentage = (scrollOffset-(y+18)).toFloat()/(y+(size.rows*18)+1-(y+18)).toFloat()
        return (scrollPercentage/(1f/ scrollPages)).toInt()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        val scrollPage = getScrollPage()
        if(amount > 0) {
            if(scrollPage > 0) {
                val desiredPercentage = (scrollPage-1f)/scrollPages
                scrollOffset = MathHelper.ceil((desiredPercentage*(y+(size.rows*18)+1-(y+18)) + (y+18)))
            }
        }else{
            if(scrollPage < scrollPages) {
                val desiredPercentage = (scrollPage+1f)/scrollPages
                scrollOffset = MathHelper.ceil(desiredPercentage*(y+(size.rows*18)+1-(y+18)) + (y+18))
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
        this.itemRenderer.renderInGuiWithOverrides(this.client!!.player, slot.item, i, j)

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
            if(playerInventory.cursorStack.isEmpty) {
                val tooltip = getTooltipFromItem(slot.item)
                tooltip.add(1, LiteralText("${Formatting.GRAY}Stored: ${slot.count}"))
                renderTooltip(matrices, tooltip, mouseX, mouseY)
            }
        }
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(size.texture)
        if(size == Size.AUTOMATIC) {
            drawTexture(matrices, x, y, 0, 0, 193, 35)
            (0 until size.rows-2).forEach {
                drawTexture(matrices, x, y+35+it*18, 0, 35, 193, 18)
            }
            drawTexture(matrices, x, y+35+(size.rows-2)*18, 0, 125, 193, 114)
        }else{
            drawTexture(matrices, x, y, 0, 0, bWidth, bHeight)
        }

    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        val i = (size.rows-5)
        textRenderer.draw(matrices, title.string, 6f, 6f, 4210752)
        textRenderer.draw(matrices, playerInventory.displayName, 6f,  110f+(i*18), 4210752)
    }


}