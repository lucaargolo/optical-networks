package io.github.lucaargolo.opticalnetworks.utils.widgets

import net.minecraft.item.ItemStack

class TerminalSlot(val x: Int, val y: Int) {

    var item: ItemStack = ItemStack.EMPTY
    var count: Int = 0
    var craftable: Boolean = false

    fun getRenderString(): String {
        return if(craftable && count == -1) "Craft" else when {
            count <= 999 -> count.toString()
            count <= 999999 -> {
                val s = ((count *10)/1000).toString()
                s.substring(0, s.length-1)+"."+s.substring(s.length-1)+"K"
            }
            else -> {
                val s = ((count *10)/1000000).toString()
                s.substring(0, s.length-1)+"."+s.substring(s.length-1)+"M"
            }
        }
    }


}