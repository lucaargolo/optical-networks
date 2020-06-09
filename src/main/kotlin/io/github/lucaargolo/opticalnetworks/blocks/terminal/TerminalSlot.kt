package io.github.lucaargolo.opticalnetworks.blocks.terminal

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

class TerminalSlot(val x: Int, val y: Int) {

    var item: ItemStack = ItemStack.EMPTY
    var count: Int = 0;

    fun getCountString(): String {
        if(count <= 999) return count.toString()
        else if(count <= 999999) return (count/1000.0).toString()+"K"
        else return (count/1000000.0).toString()+"M"
    }


}