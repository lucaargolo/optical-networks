package io.github.lucaargolo.opticalnetworks.utils.autocrafting

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.blocks.terminal.TerminalScreen
import io.github.lucaargolo.opticalnetworks.packets.CONFIRM_CRAFTING_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.utils.areStacksCompatible
import io.github.lucaargolo.opticalnetworks.utils.TagStack
import io.github.lucaargolo.opticalnetworks.utils.widgets.ScrollButtonWidget
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper

class ConfirmCraftScreen(val terminal: TerminalScreen, val action: CraftingAction): Screen(LiteralText("Crafting")) {

    private val texture = Identifier("opticalnetworks:textures/gui/confirm_crafting.png")

    private var scrollPages = 1
    private var scrollOffset = 0
    private var scrollable = false
    private var isScrolling = false
    private var scrollButton: ScrollButtonWidget? = null

    private var cancelButton: ButtonWidget? = null
    private var craftButton: ButtonWidget? = null

    private val backgroundWidth = 136
    private val backgroundHeight = 176

    var x = (width - backgroundWidth) / 2
    var y = (height - backgroundHeight) / 2

    private val inputStacks = mutableListOf<ItemStack>()
    private val craftStacks = mutableListOf<ItemStack>()
    private val missingStacks = mutableListOf<ItemStack>()
    private val missingTags = mutableListOf<TagStack>()

    private fun addStacks(action: CraftingAction) {
        action.availableStacks.forEach { inputStack ->
            var found = false
            inputStacks.forEach stacks@{ stk ->
                if(areStacksCompatible(inputStack, stk)) {
                    found = true
                    stk.increment(inputStack.count*action.quantity)
                    return@stacks
                }
            }
            if(!found) {
                val dummyStack = inputStack.copy()
                dummyStack.count *= action.quantity
                inputStacks.add(dummyStack)
            }
        }
        action.missingStacks.forEach { missingStack ->
            var found = false
            missingStacks.forEach stacks@{ stk ->
                if(areStacksCompatible(missingStack, stk)) {
                    found = true
                    stk.increment(missingStack.count)
                    //stk.increment(missingStack.count*action.quantity)
                    return@stacks
                }
            }
            if(!found) {
                val dummyStack = missingStack.copy()
                //dummyStack.count *= action.quantity
                missingStacks.add(dummyStack)
            }
        }
        action.missingTags.forEach { missingTag ->
            var found = false
            missingTags.forEach stacks@{ tag ->
                if(missingTag.mcTag == tag.mcTag) {
                    found = true
                    tag.count += missingTag.count*action.quantity
                    return@stacks
                }
            }
            if(!found) {
                val dummyTag = missingTag.copy()
                dummyTag.count *= action.quantity
                missingTags.add(dummyTag)
            }
        }
        action.necessaryActions.forEach {
            it.outputStacks.forEach { outputStack ->
                var found = false
                craftStacks.forEach stacks@{ stk ->
                    if(areStacksCompatible(outputStack, stk)) {
                        found = true
                        stk.increment(outputStack.count*it.quantity)
                        return@stacks
                    }
                }
                if(!found) {
                    val dummyStack = outputStack.copy()
                    dummyStack.count = outputStack.count*it.quantity
                    craftStacks.add(dummyStack)
                }
            }
            addStacks(it)
        }
    }

    init {
        addStacks(action)
    }

    override fun init() {
        super.init()
        x = (width - backgroundWidth) / 2
        y = (height - backgroundHeight) / 2
        cancelButton = ButtonWidget(x+6, y+150, 60, 20, LiteralText("Cancel"), ButtonWidget.PressAction { onClose() })
        this.addButton(cancelButton)
        craftButton = ButtonWidget(x+70, y+150, 60, 20, LiteralText("Craft"), ButtonWidget.PressAction {
            val passedData = PacketByteBuf(Unpooled.buffer())
            ClientSidePacketRegistry.INSTANCE.sendToServer(CONFIRM_CRAFTING_C2S_PACKET, passedData)
            onClose()
        })
        this.addButton(craftButton)
        scrollOffset = y + 17
        scrollButton = ScrollButtonWidget(x + 119, y + 17, ButtonWidget.PressAction { if(scrollable) isScrolling = true })
        this.addButton(scrollButton)
    }

    var itemDelta = 0f

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        this.drawBackground(matrices)
        if(missingTags.isNotEmpty() || missingStacks.isNotEmpty()) {
            craftButton?.active = false
        }
        super.render(matrices, mouseX, mouseY, delta)
        scrollButton?.isPressed = this.isScrolling
        scrollButton?.y = scrollOffset

        this.drawForeground(matrices)
        RenderSystem.enableDepthTest()

        val stackMap = mutableMapOf<ItemStack, String>()
        val tagsMap = mutableMapOf<String, TagStack>()
        missingTags.forEach {
            val renderStacks = it.renderStacks
            var renderIndex = MathHelper.ceil(itemDelta)
            if(renderIndex >= renderStacks.size) {
                renderIndex = 0
                itemDelta = 0f
            }
            stackMap[renderStacks[renderIndex]] = it.mcTag
            tagsMap[it.mcTag] = it
        }
        missingStacks.forEach { stackMap[it] = "missing" }
        craftStacks.forEach { stackMap[it] = "craft" }
        inputStacks.forEach { stackMap[it] = "input" }

        val activeSlots = stackMap.size
        scrollable = activeSlots > 5
        scrollPages = activeSlots - 5

        val scrollPage = getScrollPage()

        var index = 0
        var indexPage = 0
        stackMap.forEach { (stk, type) ->
            if(indexPage >= scrollPage) {
                if(index < 5) {
                    this.itemRenderer.renderInGuiWithOverrides(this.client!!.player, stk, x+6, y+17+(index*18))
                    when(type) {
                        "missing" -> {
                            DrawableHelper.fill(matrices, x+6, y+17+(index*18), x+6+106, y+17+(index*18)+16, 0x44FF4444)
                            matrices.scale(0.5F, 0.5F, 1.0F)
                            textRenderer.draw(matrices, TranslatableText(stk.translationKey), (x+25f)*2, (y+20f+(index*18))*2, 4210752)
                            textRenderer.draw(matrices, LiteralText("Missing: ${stk.count}"), (x+25f)*2, (y+20f+(index*18)+5f)*2, 4210752)
                            matrices.scale(2.0F, 2.0F, 1.0F)
                        }
                        "craft" -> {
                            DrawableHelper.fill(matrices, x+6, y+17+(index*18), x+6+106, y+17+(index*18)+16, 0x4444FF44)
                            matrices.scale(0.5F, 0.5F, 1.0F)
                            textRenderer.draw(matrices, TranslatableText(stk.translationKey), (x+25f)*2, (y+20f+(index*18))*2, 4210752)
                            textRenderer.draw(matrices, LiteralText("To craft: ${stk.count}"), (x+25f)*2, (y+20f+(index*18)+5f)*2, 4210752)
                            matrices.scale(2.0F, 2.0F, 1.0F)
                        }
                        "input" -> {
                            matrices.scale(0.5F, 0.5F, 1.0F)
                            textRenderer.draw(matrices, TranslatableText(stk.translationKey), (x+25f)*2, (y+20f+(index*18))*2, 4210752)
                            textRenderer.draw(matrices, LiteralText("Required: ${stk.count}"), (x+25f)*2, (y+20f+(index*18)+5f)*2, 4210752)
                            matrices.scale(2.0F, 2.0F, 1.0F)
                        }
                        else -> {
                            DrawableHelper.fill(matrices, x+6, y+17+(index*18), x+6+106, y+17+(index*18)+16, 0x44FF4444)
                            matrices.scale(0.5F, 0.5F, 1.0F)
                            textRenderer.draw(matrices, LiteralText(type), (x+25f)*2, (y+20f+(index*18))*2, 4210752)
                            textRenderer.draw(matrices, LiteralText("Missing: ${tagsMap[type]!!.count}"), (x+25f)*2, (y+20f+(index*18)+5f)*2, 4210752)
                            matrices.scale(2.0F, 2.0F, 1.0F)
                        }
                    }
                }
                index++
            }else{
                indexPage++
            }
        }

        this.itemRenderer.renderInGuiWithOverrides(this.client!!.player, action.outputStacks[0], x+11, y+120)
        textRenderer.draw(matrices, TranslatableText(action.outputStacks[0].translationKey), x+35f, y+119f, 4210752)
        textRenderer.draw(matrices, LiteralText("To craft: ${action.outputStacks[0].count*action.quantity}"), x+35f, y+129f, 4210752)

        if(mouseX in (x+11..x+27) && mouseY in (y+120..y+136)) {
            DrawableHelper.fill(matrices, x+11, y+120, x+27, y+136, -2130706433)
            renderTooltip(matrices, action.outputStacks[0], mouseX, mouseY)
        }

        itemDelta += delta*0.1f
    }

    private fun getScrollPage(): Int {
        val scrollPercentage = (scrollOffset-(y+17)).toFloat()/(y+91-(y+17)).toFloat()
        return (scrollPercentage/(1f/ scrollPages)).toInt()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        val scrollPage = getScrollPage()
        if(amount > 0) {
            if(scrollPage > 0) {
                val desiredPercentage = (scrollPage-1f)/scrollPages
                scrollOffset = MathHelper.ceil((desiredPercentage*(y+91-(y+17)) + (y+17)))
            }
        }else{
            if(scrollPage < scrollPages) {
                val desiredPercentage = (scrollPage+1f)/scrollPages
                scrollOffset = MathHelper.ceil(desiredPercentage*(y+91-(y+17)) + (y+17))
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return if (isScrolling) {
            scrollOffset = MathHelper.clamp(mouseY.toInt(), y+17, y+91)
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

    private fun drawBackground(matrices: MatrixStack?) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(texture)
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight)
    }

    private fun drawForeground(matrices: MatrixStack?) {
        val text = LiteralText("Crafting")
        textRenderer.draw(matrices, text, x+(backgroundWidth/2)-(textRenderer.getWidth(text)/2f), y+6f, 4210752)
    }

    override fun onClose() {
        client!!.openScreen(terminal)
    }

    override fun isPauseScreen() = false

}