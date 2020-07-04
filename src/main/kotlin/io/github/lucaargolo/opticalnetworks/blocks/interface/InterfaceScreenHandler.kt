package io.github.lucaargolo.opticalnetworks.blocks.`interface`

import io.github.lucaargolo.opticalnetworks.blocks.attachment.AttachmentScreenHandler
import io.github.lucaargolo.opticalnetworks.items.blueprint.Blueprint
import io.github.lucaargolo.opticalnetworks.utils.BlockEntityInventory
import io.github.lucaargolo.opticalnetworks.utils.widgets.GhostSlot
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.slot.Slot

class InterfaceScreenHandler(syncId: Int, playerInventory: PlayerInventory, entity: InterfaceBlockEntity, context: ScreenHandlerContext): AttachmentScreenHandler(syncId, playerInventory, entity, context), GhostSlot.IScreenHandler {

    var inventory: Inventory = BlockEntityInventory(this, entity)

    override fun addGhostSlots() {
        (0..2).forEach { n ->
            (0..2).forEach { m ->
                ghostSlots.add(GhostSlot(m + (n * 3), 7 + (m + 1) * 18, 18 + n * 18 + 16))
            }
        }

        (0..2).forEach { n ->
            (0..2).forEach { m ->
                ghostSlots.add(GhostSlot(9 + m + (n * 3), 7 + (m + 5) * 18, 18 + n * 18 + 16))
            }
        }
    }

    override fun addPlayerSlots() {
        (0..2).forEach { n ->
            (0..8).forEach { m ->
                addSlot(Slot(playerInventory, m + n * 9 + 9, 8 + m * 18, 54 + 84 + n * 18))
            }
        }

        (0..8).forEach { n ->
            addSlot(Slot(playerInventory, n, 8 + n * 18, 54 + 142))
        }
    }

    init {
        (0..8).forEach { n ->
            addSlot(object: Slot(inventory, n,  8 + n * 18, 23 + 84) {
                override fun canInsert(stack: ItemStack): Boolean {
                    return (stack.item is Blueprint && stack.hasTag() && stack.tag!!.contains("type"))
                }
            })
        }
    }


}