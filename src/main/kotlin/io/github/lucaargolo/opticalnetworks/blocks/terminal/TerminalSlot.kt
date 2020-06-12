package io.github.lucaargolo.opticalnetworks.blocks.terminal

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import java.text.DecimalFormat

class TerminalSlot(val x: Int, val y: Int) {

    var item: ItemStack = ItemStack.EMPTY
    var count: Int = 0;

    fun getCountString(): String {
        val string = when {
            count <= 999 -> count.toString()
            count <= 999999 -> {
                val s = ((count*10)/1000).toString()
                s.substring(0, s.length-1)+"."+s.substring(s.length-1)+"K"
            }
            else -> {
                val s = ((count*10)/1000000).toString()
                s.substring(0, s.length-1)+"."+s.substring(s.length-1)+"M"
            }
        }
        return  string
    }


}