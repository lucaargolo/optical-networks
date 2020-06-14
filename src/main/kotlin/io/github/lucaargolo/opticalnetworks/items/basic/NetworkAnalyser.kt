package io.github.lucaargolo.opticalnetworks.items.basic

import io.github.lucaargolo.opticalnetworks.network.blocks.NetworkConnectable
import io.github.lucaargolo.opticalnetworks.network.NetworkState
import io.github.lucaargolo.opticalnetworks.network.getNetworkState
import net.minecraft.item.Item
import net.minecraft.item.ItemUsageContext
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.LiteralText
import net.minecraft.util.ActionResult

class NetworkAnalyser(settings: Settings): Item(settings) {

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val blockState = context.world.getBlockState(context.blockPos)
        return if(blockState.block is NetworkConnectable) {
            if(!context.world.isClient) {
                val networkState = getNetworkState(context.world as ServerWorld)
                val network = networkState.getNetwork(context.world as ServerWorld, context.blockPos)
                if(network == null) {
                    context.player?.sendMessage(LiteralText("No network my dude"), false)
                }else{
                    context.player?.sendMessage(LiteralText("Found network with ${network.components.size} components"), false)
                    println("Current state: ")
                    networkState.networks.forEach {
                        println("Network ${it.id}")
                        it.components.forEachIndexed { index, pair ->
                            println("$index: ${pair}")
                        }
                    }
                }
            }
            ActionResult.SUCCESS
        }else super.useOnBlock(context)
    }

}