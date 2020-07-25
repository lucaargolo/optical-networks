package io.github.lucaargolo.opticalnetworks.blocks.terminal

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.mixin.SlotMixin
import io.github.lucaargolo.opticalnetworks.packets.NETWORK_INTERACT_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.packets.UPDATE_TERMINAL_CONFIG_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.packets.terminalConfig
import io.github.lucaargolo.opticalnetworks.utils.*
import io.github.lucaargolo.opticalnetworks.utils.autocrafting.RequestCraftScreen
import io.github.lucaargolo.opticalnetworks.utils.widgets.EnumButtonWidget
import io.github.lucaargolo.opticalnetworks.utils.widgets.ScrollButtonWidget
import io.github.lucaargolo.opticalnetworks.utils.widgets.TerminalSlot
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.registry.Registry

open class TerminalScreen(handler: ScreenHandler, inventory: PlayerInventory, title: Text): ModHandledScreen<ScreenHandler>(handler, inventory, title) {

    private var texture = Identifier("opticalnetworks:textures/gui/terminal.png")

    private var scrollPages = 1
    private var scrollOffset = 0
    private var scrollable = false
    private var isScrolling = false
    private var activeSlots = 0
    private var scrollButton: ScrollButtonWidget? = null

    private var sizeButton: EnumButtonWidget<TerminalConfig.Size>? = null
    private var sortButton: EnumButtonWidget<TerminalConfig.Sort>? = null
    private var sortDirectionButton: EnumButtonWidget<TerminalConfig.SortDirection>? = null

    private var searchBox: TextFieldWidget? = null

    protected var bWidth = 0
    protected var bHeight = 0

    private var hoverTerminalSlot: TerminalSlot? = null
    var hdl = handler as Terminal.IScreenHandler

    override fun tick() {
        searchBox?.tick()
    }

    private fun updateTerminalConfig() {
        val passedData = PacketByteBuf(Unpooled.buffer())
        passedData.writeCompoundTag(terminalConfig.toTag(CompoundTag()))
        ClientSidePacketRegistry.INSTANCE.sendToServer(UPDATE_TERMINAL_CONFIG_C2S_PACKET, passedData)
    }

    open fun repositionSlots() {
        val slotIterator = handler.slots.iterator()
        while(slotIterator.hasNext()) {
            val slot = slotIterator.next()
            when(slot.id) {
                in (0..26) -> (slot as SlotMixin).setY(103 + (slot.id/9) * 18 + 18*(terminalConfig.size.rows-4))
                else -> (slot as SlotMixin).setY(161 + 18*(terminalConfig.size.rows-4))
            }
        }
    }

    open fun updateSize() {
        if(terminalConfig.size == TerminalConfig.Size.AUTOMATIC) {
            terminalConfig.size.rows = (height-144)/18
            if(terminalConfig.size.rows < 3) terminalConfig.size.rows = 3
            terminalConfig.size.y = 113+(18*terminalConfig.size.rows)
        }

        bWidth = terminalConfig.size.x
        bHeight = terminalConfig.size.y
        x = (width - bWidth) / 2
        y = (height - bHeight) / 2
    }

    open fun getSearchBoxSize() = 80

    override fun init() {
        super.init()

        updateSize()
        repositionSlots()

        if(hdl.terminalSlots.size > terminalConfig.size.rows*9) {
            val it = hdl.terminalSlots.iterator()
            var index = 0
            while(it.hasNext()) {
                it.next()
                if(index >= terminalConfig.size.rows*9) it.remove()
                index++
            }
        }

        if(hdl.terminalSlots.size < terminalConfig.size.rows*9) {
            val i = 18 + 18 * hdl.terminalSlots.size/9
            (0 until terminalConfig.size.rows*9-hdl.terminalSlots.size).forEach {
                hdl.terminalSlots.add(TerminalSlot(8 + (it-((it/9)*9))*18, (it/9)*18 + i))
            }
        }

        searchBox = TextFieldWidget(textRenderer, x+81+(80-getSearchBoxSize()), y+5, getSearchBoxSize(), 9, LiteralText(""))
        searchBox!!.setMaxLength(50)
        searchBox!!.setHasBorder(false)
        searchBox!!.setEditableColor(16777215)
        this.addChild(searchBox)

        sizeButton = EnumButtonWidget(x-18, y + 4, ButtonWidget.PressAction {
            sizeButton?.change()
            sizeButton?.state?.let {
                terminalConfig.size = it
            }
            updateTerminalConfig()
            init(client, width, height)
        }, terminalConfig.size, TerminalConfig.Size.values(), texture, 193, 0)
        this.addButton(sizeButton)

        sortButton = EnumButtonWidget(x-18, y + 22, ButtonWidget.PressAction {
            sortButton?.change();
            sortButton?.state?.let{
                terminalConfig.sort = it
            }
            updateTerminalConfig()
        }, terminalConfig.sort, TerminalConfig.Sort.values(), texture, 193, 16)
        this.addButton(sortButton)

        sortDirectionButton = EnumButtonWidget(x-18, y + 40, ButtonWidget.PressAction {
            sortDirectionButton?.change()
            sortDirectionButton?.state?.let {
                terminalConfig.sortDirection = it
            }
            updateTerminalConfig()
        }, terminalConfig.sortDirection, TerminalConfig.SortDirection.values(), texture, 193, 32)
        this.addButton(sortDirectionButton)

        scrollOffset = y + 18
        scrollButton = ScrollButtonWidget(x + 175, y + 18, ButtonWidget.PressAction {})
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
        hdl.terminalSlots.forEach { slot: TerminalSlot ->
            val i = slot.x + x - 1
            val j = slot.y + y - 1
            if(!hdl.network.isValid()) DrawableHelper.fill(matrices, x+slot.x, y+slot.y, x+slot.x+16, y+slot.y+16, 0x60666666)
            else if(mouseX in (i..i+18) && mouseY in (j..j+18)) hoverTerminalSlot = slot
            slot.item = ItemStack.EMPTY
            slot.count = 0
        }

        val stacksAndCraftablesMap = mutableMapOf<ItemStack, Boolean>()

        val allCraftables = hdl.network.getAvailableCraftables(searchBox?.text ?: "")
        val allStacks = hdl.network.getAvailableStacks(searchBox?.text ?: "")

        allCraftables.forEach { craftableStack ->
            stacksAndCraftablesMap[craftableStack] = true
        }

        allStacks.forEach { storedStack ->
            var found = false
            val iterator = stacksAndCraftablesMap.iterator()
            while(iterator.hasNext()) {
                val entry = iterator.next()
                if(entry.value && areStacksCompatible(storedStack, entry.key)) {
                    found = true
                    iterator.remove()
                    stacksAndCraftablesMap[storedStack] = true
                    break
                }
            }
            if(!found) stacksAndCraftablesMap[storedStack] = false
        }

        var sortedStacks = when(sortButton?.state) {
            TerminalConfig.Sort.NAME -> stacksAndCraftablesMap.keys.sortedBy { TranslatableText(it.translationKey).string }
            TerminalConfig.Sort.QUANTITY -> stacksAndCraftablesMap.keys.sortedBy { it.count }.reversed()
            TerminalConfig.Sort.ID -> stacksAndCraftablesMap.keys.sortedBy { Registry.ITEM.getRawId(it.item) }
            else -> stacksAndCraftablesMap.keys
        }

        if(sortDirectionButton?.state == TerminalConfig.SortDirection.DESCENDING) sortedStacks = sortedStacks.reversed()

        activeSlots = sortedStacks.size
        scrollable = activeSlots/9f > (terminalConfig.size.rows).toFloat()
        scrollPages = (MathHelper.ceil(activeSlots/9f) - terminalConfig.size.rows)

        val scrollPage = getScrollPage()

        var index = 0
        var indexPage = 0
        sortedStacks.forEach {
            if(indexPage >= 9*scrollPage) {
                val slot = hdl.terminalSlots.getOrNull(index)
                if(slot != null) {
                    slot.item = it
                    slot.count = if(allCraftables.contains(it)) -1 else it.count
                    slot.craftable = stacksAndCraftablesMap[it]!!
                    drawTerminalSlot(matrices, slot, mouseX, mouseY)
                }
                index++
            }else{
                indexPage++
            }
        }

        drawMouseoverTooltip(matrices, mouseX, mouseY)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return if (isScrolling) {
            scrollOffset = MathHelper.clamp(mouseY.toInt(), y+18, y+(terminalConfig.size.rows*18)+1)
            true
        } else {
            super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        }
    }

    private fun getScrollPage(): Int {
        val scrollPercentage = (scrollOffset-(y+18)).toFloat()/(y+(terminalConfig.size.rows*18)+1-(y+18)).toFloat()
        return (scrollPercentage/(1f/ scrollPages)).toInt()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        val scrollPage = getScrollPage()
        if(amount > 0) {
            if(scrollPage > 0) {
                val desiredPercentage = (scrollPage-1f)/scrollPages
                scrollOffset = MathHelper.ceil((desiredPercentage*(y+(terminalConfig.size.rows*18)+1-(y+18)) + (y+18)))
            }
        }else{
            if(scrollPage < scrollPages) {
                val desiredPercentage = (scrollPage+1f)/scrollPages
                scrollOffset = MathHelper.ceil(desiredPercentage*(y+(terminalConfig.size.rows*18)+1-(y+18)) + (y+18))
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isScrolling = false
        isScrolling = if(scrollButton != null) (scrollable && mouseX > scrollButton!!.x && mouseX < scrollButton!!.x+10 && mouseY > scrollButton!!.y && mouseY <= scrollButton!!.y+15) else isScrolling
        if(hoverTerminalSlot != null && button in (0..1)) {
            if(hoverTerminalSlot!!.count == -1 && playerInventory.cursorStack.isEmpty) {
                //Requesting an auto craft action
                client!!.openScreen(RequestCraftScreen(this, hoverTerminalSlot!!.item))
            }else{
                //Requesting a regular item from the system
                val passedData = PacketByteBuf(Unpooled.buffer())
                passedData.writeUuid(hdl.network.id)
                passedData.writeInt(1)
                passedData.writeInt(button)
                passedData.writeBoolean(InputUtil.isKeyPressed(client!!.window.handle, 340) || InputUtil.isKeyPressed(client!!.window.handle, 344))
                val placeholderStack = hoverTerminalSlot!!.item.copy()
                if (placeholderStack.count > placeholderStack.maxCount) placeholderStack.count = placeholderStack.maxCount
                passedData.writeItemStack(placeholderStack)
                ClientSidePacketRegistry.INSTANCE.sendToServer(NETWORK_INTERACT_C2S_PACKET, passedData)
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
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

        val string = slot.getRenderString()
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
                if(slot.count != -1) tooltip.add(1, LiteralText("${Formatting.GRAY}Stored: ${slot.count}"))
                renderTooltip(matrices, tooltip, mouseX, mouseY)
            }
        }
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(texture)
        drawTexture(matrices, x, y, 0, 0, 193, 35)
        (0 until terminalConfig.size.rows-2).forEach {
            drawTexture(matrices, x, y+35+it*18, 0, 35, 193, 18)
        }
        drawTexture(matrices, x, y+35+(terminalConfig.size.rows-2)*18, 0, 125, 193, 114)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        val i = (terminalConfig.size.rows-5)
        textRenderer.draw(matrices, title.string, 6f, 6f, 4210752)
        textRenderer.draw(matrices, playerInventory.displayName, 6f,  110f+(i*18), 4210752)
    }


}