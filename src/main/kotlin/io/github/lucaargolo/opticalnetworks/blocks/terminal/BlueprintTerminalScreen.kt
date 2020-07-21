package io.github.lucaargolo.opticalnetworks.blocks.terminal

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.mixin.SlotMixin
import io.github.lucaargolo.opticalnetworks.packets.*
import io.github.lucaargolo.opticalnetworks.utils.widgets.GhostSlot
import io.github.lucaargolo.opticalnetworks.utils.widgets.PressableWidget
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier

@Suppress("DuplicatedCode")
abstract class BlueprintTerminalScreen(handler: BlueprintTerminalScreenHandler, inventory: PlayerInventory, title: Text): CraftingTerminalScreen(handler, inventory, title) {

    class Crafting(handler: BlueprintTerminalScreenHandler, inventory: PlayerInventory, title: Text): BlueprintTerminalScreen(handler, inventory, title) {

        override var texture2 = Identifier("opticalnetworks:textures/gui/blueprint_terminal_crafting.png")
        override var name = "crafting"
        override var oppositeName = "processing"

        override fun repositionSlots() {
            val slotIterator = handler.slots.iterator()
            while(slotIterator.hasNext()) {
                val slot = slotIterator.next()
                when(slot.id) {
                    0 -> (slot as SlotMixin).setY(94 + 18*(terminalConfig.size.rows-3))
                    in (1..27) -> (slot as SlotMixin).setY(103 + ((slot.id-1)/9) * 18 + 18*(terminalConfig.size.rows-4)+58)
                    in (28..36) -> (slot as SlotMixin).setY(161 + 18*(terminalConfig.size.rows-4)+58)
                    37 -> (slot as SlotMixin).setY(76 + 18*(terminalConfig.size.rows-3))
                    else -> (slot as SlotMixin).setY(76 + 18*(terminalConfig.size.rows-1))
                }
            }
            val ghostSlotIterator = (this.handler as GhostSlot.IScreenHandler).ghostSlots.iterator()
            while(ghostSlotIterator.hasNext()) {
                val ghostSlot = ghostSlotIterator.next()
                ghostSlot.y = 75 + (ghostSlot.index/3) * 18 + 18*(terminalConfig.size.rows-3)
            }
        }

    }

    class Processing(handler: BlueprintTerminalScreenHandler, inventory: PlayerInventory, title: Text): BlueprintTerminalScreen(handler, inventory, title) {

        override var texture2 = Identifier("opticalnetworks:textures/gui/blueprint_terminal_processing.png")
        override var name = "processing"
        override var oppositeName = "crafting"

        override fun repositionSlots() {
            val slotIterator = handler.slots.iterator()
            while(slotIterator.hasNext()) {
                val slot = slotIterator.next()
                when(slot.id) {
                    in (0..26) -> (slot as SlotMixin).setY(103 + ((slot.id)/9) * 18 + 18*(terminalConfig.size.rows-4)+58)
                    in (27..35) -> (slot as SlotMixin).setY(161 + 18*(terminalConfig.size.rows-4)+58)
                    36 -> (slot as SlotMixin).setY(76 + 18*(terminalConfig.size.rows-3))
                    else -> (slot as SlotMixin).setY(76 + 18*(terminalConfig.size.rows-1))
                }
            }
            val ghostSlotIterator = (this.handler as GhostSlot.IScreenHandler).ghostSlots.iterator()
            while(ghostSlotIterator.hasNext()) {
                val ghostSlot = ghostSlotIterator.next()
                when(ghostSlot.index) {
                    in (0..8) -> ghostSlot.y = 75 + (ghostSlot.index/3) * 18 + 18*(terminalConfig.size.rows-3)
                    else -> ghostSlot.y = 93 + 18*((terminalConfig.size.rows-5)+(ghostSlot.index-8))
                }
            }
        }

    }

    private var changeMode: ButtonWidget? = null
    private var changeItemTagMode: ButtonWidget? = null
    private var changeNbtTagMode: ButtonWidget? = null

    abstract var name: String
    abstract var oppositeName: String

    override fun init() {
        super.init()
        changeMode = object: ButtonWidget(x+15, y+85+18*(terminalConfig.size.rows-3), 8, 8, LiteralText(""), PressAction{
            (changeMode as PressableWidget).isPressed = true
            ClientSidePacketRegistry.INSTANCE.sendToServer(CHANGE_BLUEPRINT_MODE_C2S_PACKET, PacketByteBuf(Unpooled.buffer()))
        }), PressableWidget {
            override var isPressed = false
            override fun renderButton(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
                RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
                MinecraftClient.getInstance().textureManager.bindTexture(texture2)
                if(isPressed) drawTexture(matrices, x, y, 201, 8, 8, 8)
                else drawTexture(matrices, x, y, 193, 8, 8, 8)
                if(isPressed || isHovered) DrawableHelper.fill(matrices, x + 1, y + 1, x + 7, y + 7, -2130706433)
            }
        }
        this.addButton(changeMode!!)
        changeItemTagMode = object: ButtonWidget(x+15, y+95+18*(terminalConfig.size.rows-3), 8, 8, LiteralText(""), PressAction{
            (changeItemTagMode as PressableWidget).isPressed = true
            ClientSidePacketRegistry.INSTANCE.sendToServer(CHANGE_BLUEPRINT_ITEM_TAG_MODE_C2S_PACKET, PacketByteBuf(Unpooled.buffer()))
        }), PressableWidget {
            override var isPressed = false
            override fun renderButton(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
                RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
                MinecraftClient.getInstance().textureManager.bindTexture(texture2)
                if(((handler as BlueprintTerminalScreenHandler).entity as BlueprintTerminalBlockEntity).useItemTag) {
                    if(isPressed) drawTexture(matrices, x, y, 201, 16, 8, 8)
                    else drawTexture(matrices, x, y, 193, 16, 8, 8)
                }else{
                    if(isPressed) drawTexture(matrices, x, y, 217, 16, 8, 8)
                    else drawTexture(matrices, x, y, 209, 16, 8, 8)
                }
                if(isPressed || isHovered) DrawableHelper.fill(matrices, x + 1, y + 1, x + 7, y + 7, -2130706433)
            }
        }
        this.addButton(changeItemTagMode!!)
        changeNbtTagMode = object: ButtonWidget(x+15, y+105+18*(terminalConfig.size.rows-3), 8, 8, LiteralText(""), PressAction{
            (changeNbtTagMode as PressableWidget).isPressed = true
            ClientSidePacketRegistry.INSTANCE.sendToServer(CHANGE_BLUEPRINT_NBT_TAG_MODE_C2S_PACKET, PacketByteBuf(Unpooled.buffer()))
        }), PressableWidget {
            override var isPressed = false
            override fun renderButton(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
                RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
                MinecraftClient.getInstance().textureManager.bindTexture(texture2)
                if(((handler as BlueprintTerminalScreenHandler).entity as BlueprintTerminalBlockEntity).useNbtTag) {
                    if(isPressed) drawTexture(matrices, x, y, 201, 24, 8, 8)
                    else drawTexture(matrices, x, y, 193, 24, 8, 8)
                }else{
                    if(isPressed) drawTexture(matrices, x, y, 217, 24, 8, 8)
                    else drawTexture(matrices, x, y, 209, 24, 8, 8)
                }
                if(isPressed || isHovered) DrawableHelper.fill(matrices, x + 1, y + 1, x + 7, y + 7, -2130706433)
            }
        }
        this.addButton(changeNbtTagMode!!)
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(matrices, mouseX, mouseY, delta)
        this.buttons.forEach {
            if(it == changeMode) {
                if(it.isHovered) {
                    val tooltip = mutableListOf<Text>()
                    tooltip.add(LiteralText("tooltip.opticalnetworks.$name"))
                    tooltip.add(TranslatableText("tooltip.opticalnetworks.change_to").append(LiteralText("tooltip.opticalnetworks.$oppositeName")))
                    renderTooltip(matrices, tooltip, mouseX, mouseY)
                }
            }
            if(it == changeItemTagMode) {
                if(it.isHovered) {
                    val tooltip = mutableListOf<Text>()
                    val useItemTag = ((handler as BlueprintTerminalScreenHandler).entity as BlueprintTerminalBlockEntity).useItemTag
                    val tagMode = if(useItemTag) TranslatableText("tooltip.opticalnetworks.using_item_tags") else TranslatableText("tooltip.opticalnetworks.not_using_item_tags")
                    tooltip.add(tagMode)
                    renderTooltip(matrices, tooltip, mouseX, mouseY)
                }
            }
            if(it == changeNbtTagMode) {
                if(it.isHovered) {
                    val tooltip = mutableListOf<Text>()
                    val useNbtTag = ((handler as BlueprintTerminalScreenHandler).entity as BlueprintTerminalBlockEntity).useNbtTag
                    val tagMode = if(useNbtTag) TranslatableText("tooltip.opticalnetworks.using_nbt_tag") else TranslatableText("tooltip.opticalnetworks.not_using_nbt_tag")
                    tooltip.add(tagMode)
                    renderTooltip(matrices, tooltip, mouseX, mouseY)
                }
            }
        }
    }

}