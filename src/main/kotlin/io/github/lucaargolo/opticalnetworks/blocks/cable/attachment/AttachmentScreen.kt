package io.github.lucaargolo.opticalnetworks.blocks.cable.attachment

import com.mojang.blaze3d.systems.RenderSystem
import io.github.lucaargolo.opticalnetworks.network.GHOST_SLOT_CLICK_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.network.UPDATE_CABLE_BUTTONS_C2S_PACKET
import io.github.lucaargolo.opticalnetworks.utils.EnumButtonWidget
import io.github.lucaargolo.opticalnetworks.utils.GhostSlotHandledScreen
import io.github.lucaargolo.opticalnetworks.utils.PressableWidget
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.item.TooltipContext
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class AttachmentScreen(handler: AttachmentScreenHandler, inventory: PlayerInventory, title: Text): GhostSlotHandledScreen<AttachmentScreenHandler>(handler, inventory, title) {

    private val texture = Identifier("opticalnetworks:textures/gui/cable.png")

    private var listButton: EnumButtonWidget<AttachmentBlockEntity.List>? = null
    private var nbtButton: EnumButtonWidget<AttachmentBlockEntity.Nbt>? = null
    private var damageButton: EnumButtonWidget<AttachmentBlockEntity.Damage>? = null
    private var orderButton: EnumButtonWidget<AttachmentBlockEntity.Order>? = null
    private var redstoneButton: EnumButtonWidget<AttachmentBlockEntity.Redstone>? = null


    private fun updateEntity() {
        val passedData = PacketByteBuf(Unpooled.buffer())
        passedData.writeBlockPos(handler.entity.pos)
        passedData.writeInt(AttachmentBlockEntity.List.values().indexOf(listButton?.state))
        passedData.writeInt(AttachmentBlockEntity.Nbt.values().indexOf(nbtButton?.state))
        passedData.writeInt(AttachmentBlockEntity.Damage.values().indexOf(damageButton?.state))
        passedData.writeInt(AttachmentBlockEntity.Order.values().indexOf(orderButton?.state))
        passedData.writeInt(AttachmentBlockEntity.Redstone.values().indexOf(redstoneButton?.state))
        ClientSidePacketRegistry.INSTANCE.sendToServer(UPDATE_CABLE_BUTTONS_C2S_PACKET, passedData)
    }

    override fun init() {
        super.init()
        listButton = EnumButtonWidget(x-18, y + 4, ButtonWidget.PressAction {listButton?.change(); updateEntity()}, handler.entity.listMode, AttachmentBlockEntity.List.values(), texture, 176, 0)
        this.addButton(listButton)
        nbtButton = EnumButtonWidget(x-18, y + 22, ButtonWidget.PressAction {nbtButton?.change(); updateEntity()}, handler.entity.nbtMode, AttachmentBlockEntity.Nbt.values(), texture, 176, 16)
        this.addButton(nbtButton)
        damageButton = EnumButtonWidget(x-18, y + 40, ButtonWidget.PressAction {damageButton?.change(); updateEntity()}, handler.entity.damageMode, AttachmentBlockEntity.Damage.values(), texture, 176, 32)
        this.addButton(damageButton)
        orderButton = EnumButtonWidget(x-18, y + 58, ButtonWidget.PressAction {orderButton?.change(); updateEntity()}, handler.entity.orderMode, AttachmentBlockEntity.Order.values(), texture, 176, 48)
        this.addButton(orderButton)
        redstoneButton = EnumButtonWidget(x-18, y + 76, ButtonWidget.PressAction {redstoneButton?.change(); updateEntity()}, handler.entity.redstoneMode, AttachmentBlockEntity.Redstone.values(), texture, 176, 64)
        this.addButton(redstoneButton)
    }

    override fun render(matrices: MatrixStack?, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(matrices)
        super.render(matrices, mouseX, mouseY, delta)
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

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        this.buttons.forEach {
            if(it is PressableWidget) it.isPressed = false
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        client!!.textureManager.bindTexture(texture)
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight)
    }

    override fun drawForeground(matrices: MatrixStack?, mouseX: Int, mouseY: Int) {
        textRenderer.draw(matrices, title.string, (backgroundWidth/2 - textRenderer.getWidth(title.string) / 2f), 6f, 4210752)
        textRenderer.draw(matrices, playerInventory.displayName, 8f, 74f, 4210752)
    }

}