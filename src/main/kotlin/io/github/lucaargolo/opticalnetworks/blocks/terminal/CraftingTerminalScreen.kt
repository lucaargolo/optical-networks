package io.github.lucaargolo.opticalnetworks.blocks.terminal

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.mixin.SlotMixin
import io.github.lucaargolo.opticalnetworks.network.CLEAR_TERMINAL_TABLE
import io.github.lucaargolo.opticalnetworks.network.terminalConfig
import io.github.lucaargolo.opticalnetworks.utils.PressableWidget
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Identifier

open class CraftingTerminalScreen(handler: ScreenHandler, inventory: PlayerInventory, title: Text): TerminalScreen(handler, inventory, title) {

    open var texture2 = Identifier("opticalnetworks:textures/gui/crafting_terminal.png")

    private var clearTable: ButtonWidget? = null

    override fun init() {
        super.init()
        clearTable = object: ButtonWidget(x+15, y+75, 8, 8, LiteralText(""), PressAction{
            (clearTable as PressableWidget).isPressed = true
            ClientSidePacketRegistry.INSTANCE.sendToServer(CLEAR_TERMINAL_TABLE, PacketByteBuf(Unpooled.buffer()))
        }), PressableWidget {
            override var isPressed = false
            override fun renderButton(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
                RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
                MinecraftClient.getInstance().textureManager.bindTexture(texture2)
                if(isPressed) drawTexture(matrices, x, y, 201, 0, 8, 8)
                else drawTexture(matrices, x, y, 193, 0, 8, 8)
                if(isPressed || isHovered) DrawableHelper.fill(matrices, x + 1, y + 1, x + 7, y + 7, -2130706433)
            }
        }
        this.addButton(clearTable!!)
    }

    override fun updateSize() {
        if(terminalConfig.size == TerminalConfig.Size.AUTOMATIC) {
            terminalConfig.size.rows = (height-204)/18
            if(terminalConfig.size.rows < 3) terminalConfig.size.rows = 3
            terminalConfig.size.y = 113+(18*terminalConfig.size.rows)
        }

        bWidth = terminalConfig.size.x
        bHeight = terminalConfig.size.y+71
        x = (width - bWidth) / 2
        y = (height - bHeight) / 2
    }

    override fun repositionSlots() {
        val slotIterator = handler.slots.iterator()
        while(slotIterator.hasNext()) {
            val slot = slotIterator.next()
            when(slot.id) {
                0 -> (slot as SlotMixin).setY(94 + 18*(terminalConfig.size.rows-3))
                in (1..9) -> (slot as SlotMixin).setY(76 + ((slot.id-1)/3) * 18 + 18*(terminalConfig.size.rows-3))
                in (10..36) -> (slot as SlotMixin).setY(103 + ((slot.id-10)/9) * 18 + 18*(terminalConfig.size.rows-4)+58)
                else -> (slot as SlotMixin).setY(161 + 18*(terminalConfig.size.rows-4)+58)
            }
        }
    }

    override fun getSearchBoxSixe() = 62

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(matrices, mouseX, mouseY, delta)
        this.buttons.forEach {
            if(it == clearTable) {
                if(it.isHovered) {
                    val tooltip = mutableListOf<Text>()
                    tooltip.add(LiteralText("Clear table"))
                    renderTooltip(matrices, tooltip, mouseX, mouseY)
                }
            }
        }
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(texture2)
        drawTexture(matrices, x, y, 0, 0, 193, 35)
        (0 until terminalConfig.size.rows-2).forEach {
            drawTexture(matrices, x, y+35+it*18, 0, 35, 193, 18)
        }
        drawTexture(matrices, x, y+35+(terminalConfig.size.rows-2)*18, 0, 53, 193, 172)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        val i = (terminalConfig.size.rows-5)
        textRenderer.draw(matrices, title.string, 6f, 6f, 4210752)
        textRenderer.draw(matrices, playerInventory.displayName, 6f,  110f+(i*18)+58f, 4210752)
    }

}