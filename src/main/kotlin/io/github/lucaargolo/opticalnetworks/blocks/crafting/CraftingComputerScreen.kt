package io.github.lucaargolo.opticalnetworks.blocks.crafting

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.packets.CHANGE_BLUEPRINT_MODE_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.packets.DECREASE_ACTION_PRIORITY_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.packets.INCREASE_ACTION_PRIORITY_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.packets.REMOVE_ACTION_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.utils.areStacksCompatible
import io.github.lucaargolo.opticalnetworks.utils.widgets.ScrollButtonWidget
import io.github.lucaargolo.opticalnetworks.utils.widgets.SmallButtonWidget
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ButtonWidget.PressAction
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

class CraftingComputerScreen(handler: CraftingComputerScreenHandler, inventory: PlayerInventory, title: Text): HandledScreen<CraftingComputerScreenHandler>(handler, inventory, title) {

    private val texture = Identifier("opticalnetworks:textures/gui/crafting_computer.png")

    private var scrollPages = 1
    private var scrollOffset = 0
    private var scrollable = false
    private var isScrolling = false
    private var scrollButton: ScrollButtonWidget? = null

    private var removeButtonList: MutableList<SmallButtonWidget> = mutableListOf()
    private var increasePriorityButtonList: MutableList<SmallButtonWidget> = mutableListOf()
    private var decreasePriorityButtonList: MutableList<SmallButtonWidget> = mutableListOf()

    override fun init() {
        super.init()
        backgroundHeight = 185
        x = width/2-backgroundWidth/2
        y = height/2-backgroundHeight/2
        scrollOffset = y + 18
        scrollButton = ScrollButtonWidget(x + 158, y + 18, PressAction { if(scrollable) isScrolling = true })
        this.addButton(scrollButton)
        removeButtonList = mutableListOf()
        (0..3).forEach { it ->
            val button = SmallButtonWidget(x+ 143, y + 18+(it*18), 176, 0, PressAction { btn ->
                val attachedData = PacketByteBuf(Unpooled.buffer())
                attachedData.writeInt((btn as SmallButtonWidget).data)
                attachedData.writeBlockPos(handler.entity.pos)
                ClientSidePacketRegistry.INSTANCE.sendToServer(REMOVE_ACTION_C2S_PACKET, attachedData)
            }, texture)
            removeButtonList.add(button)
            this.addButton(button)
        }
        increasePriorityButtonList = mutableListOf()
        (0..3).forEach {
            val button = SmallButtonWidget(x + 81, y + 18+(it*18), 176, 8, PressAction { btn ->
                val attachedData = PacketByteBuf(Unpooled.buffer())
                attachedData.writeInt((btn as SmallButtonWidget).data)
                attachedData.writeBlockPos(handler.entity.pos)
                ClientSidePacketRegistry.INSTANCE.sendToServer(INCREASE_ACTION_PRIORITY_C2S_PACKET, attachedData)
            }, texture)
            increasePriorityButtonList.add(button)
            this.addButton(button)
        }
        decreasePriorityButtonList = mutableListOf()
        (0..3).forEach {
            val button = SmallButtonWidget(x + 81, y + 26+(it*18), 176, 16, PressAction { _ ->
                val attachedData = PacketByteBuf(Unpooled.buffer())
                attachedData.writeInt(it)
                attachedData.writeBlockPos(handler.entity.pos)
                ClientSidePacketRegistry.INSTANCE.sendToServer(DECREASE_ACTION_PRIORITY_C2S_PACKET, attachedData)
            }, texture)
            decreasePriorityButtonList.add(button)
            this.addButton(button)
        }
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)

        val be = handler.entity
        var tooltipToRender: MutableList<Text>? = null

        scrollButton?.isPressed = this.isScrolling
        scrollButton?.y = scrollOffset

        val activeSlots = be.cacheSortedQueue().size
        scrollable = activeSlots > 4
        scrollPages = activeSlots - 4

        val scrollPage = getScrollPage()

        var index = 0
        var indexPage = 0
        (0..3).forEach {
            removeButtonList[it].active = false
            increasePriorityButtonList[it].active = false
            decreasePriorityButtonList[it].active = false
        }
        val parentOnlyQueue = be.cacheSortedQueue().filter { !it.key.isChild }
        be.cacheSortedQueue().forEach { entry ->
            val action = entry.key
            if (indexPage >= scrollPage) {
                if (index < 4) {
                    removeButtonList[index].active = true
                    if(!action.isChild) {
                        increasePriorityButtonList[index].active = true
                        increasePriorityButtonList[index].data = parentOnlyQueue.indexOf(entry)
                        decreasePriorityButtonList[index].active = true
                        decreasePriorityButtonList[index].data = parentOnlyQueue.indexOf(entry)
                        DrawableHelper.fill(matrices, x+89, y+18+(index * 18), x+143, y+34+(index * 18), 0x4000FF00)
                        DrawableHelper.fill(matrices, x+143, y+26+(index * 18), x+151, y+34+(index * 18), 0x4000FF00)
                    }else {
                        DrawableHelper.fill(matrices, x+89, y+18+(index * 18), x+143, y+34+(index * 18), 0x400000FF)
                        DrawableHelper.fill(matrices, x+143, y+26+(index * 18), x+151, y+34+(index * 18), 0x400000FF)
                    }
                    if(mouseX in (x+81)..(x+151) && mouseY in (y+18+(index*18))..(y+34+(index*18))) {
                        val tooltip = mutableListOf<Text>()
                        tooltip.add(LiteralText("${Formatting.GOLD}Crafting"))
                        tooltip.add(TranslatableText("tooltip.opticalnetworks.quantity").formatted(Formatting.BLUE).append(LiteralText(action.quantity.toString()).formatted(Formatting.GRAY)))
                        tooltip.add(LiteralText("State: ").formatted(Formatting.BLUE).append(TranslatableText("tooltip.opticalnetworks.enum.state.${action.state.name.toLowerCase()}").formatted(Formatting.GRAY)))
                        if(action.missingStacks.isNotEmpty()) {
                            val missingStacks = LiteralText("Missing: ").formatted(Formatting.BLUE)
                            val missingStackMap = linkedMapOf<ItemStack, Int>()
                            action.missingStacks.forEachIndexed { idx, stk ->
                                var found = false
                                missingStackMap.keys.forEach{
                                    if(areStacksCompatible(it, stk)) {
                                        missingStackMap[it] = missingStackMap[it]!!+stk.count
                                        found = true
                                    }
                                }
                                if(!found) missingStackMap[stk] = stk.count
                            }
                            missingStackMap.asIterable().forEachIndexed { idx, (stk, count) ->
                                missingStacks.append(LiteralText("${Formatting.GRAY}${count}x "))
                                missingStacks.append(TranslatableText(stk.translationKey).formatted(Formatting.GRAY))
                                if(idx < missingStackMap.size-1) missingStacks.append(LiteralText("${Formatting.GRAY}, "))
                            }
                            tooltip.add(missingStacks)
                        }
                        tooltipToRender = tooltip
                    }
                    this.itemRenderer.renderInGuiWithOverrides(this.client!!.player, action.outputStacks[0], x + 89, y + 18 + (index * 18))
                    matrices.scale(0.5F, 0.5F, 1.0F)
                    textRenderer.draw(matrices, TranslatableText("tooltip.opticalnetworks.quantity").append(LiteralText(action.quantity.toString())), (x + 108f) * 2, (y + 21f + (index * 18)) * 2, 4210752)
                    textRenderer.draw(matrices, TranslatableText("tooltip.opticalnetworks.enum.state.${action.state.name.toLowerCase()}"), (x + 108f) * 2, (y + 21f + (index * 18) + 5f) * 2, 4210752)
                    matrices.scale(2.0F, 2.0F, 1.0F)
                }
                index++
            } else {
                indexPage++
            }
        }

        tooltipToRender?.let { renderTooltip(matrices, it, mouseX, mouseY) }
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