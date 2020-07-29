package io.github.lucaargolo.opticalnetworks.items.basic

import io.github.lucaargolo.opticalnetworks.network.Network
import io.github.lucaargolo.opticalnetworks.network.blocks.CableConnectable
import io.github.lucaargolo.opticalnetworks.utils.getNetworkState
import net.minecraft.item.Item
import net.minecraft.item.ItemUsageContext
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.LiteralText
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting

class NetworkAnalyser(settings: Settings): Item(settings) {

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val blockState = context.world.getBlockState(context.blockPos)
        return if(blockState.block is CableConnectable) {
            if(!context.world.isClient) {
                val networkState = getNetworkState(context.world as ServerWorld)
                val network = networkState.getNetwork(context.world as ServerWorld, context.blockPos)
                if(network == null) {
                    context.player?.sendMessage(LiteralText("${Formatting.RED}No network has been found"), false)
                }else{
                    val text = LiteralText("${Formatting.GREEN}=============[Network Analyser]=============\n")
                    text.append(LiteralText("${Formatting.BLUE}Network: ${Formatting.GOLD}${network.id.toString().substring(0, 8)}\n"))
                    text.append(LiteralText("${Formatting.BLUE}Type: ${Formatting.GOLD}${network.type.name}\n"))
                    if(network.type == Network.Type.CONTROLLER)
                        text.append(LiteralText("${Formatting.BLUE}Main Controller: ${Formatting.GOLD}(x: ${network.mainController.x}, y: ${network.mainController.y}, z: ${network.mainController.z})\n"))
                    text.append(LiteralText("${Formatting.BLUE}Components: ${Formatting.GOLD}${network.components.size}\n"))
                    val pair = network.getBandwidthStats()
                    text.append(LiteralText("${Formatting.BLUE}Bandwidth: ${Formatting.GOLD}${pair.first}/1000.0 (Penalties: ${pair.second})"))
                    text.append(LiteralText("${Formatting.BLUE}Controller Networks: ${Formatting.GOLD}${network.controllerNetworks.size}\n"))
                    network.controllerNetworks.forEach {
                        text.append(LiteralText("${Formatting.GRAY} - ${it.toString().substring(0, 8)}\n"))
                    }
                    text.append(LiteralText("${Formatting.BLUE}Component Networks: ${Formatting.GOLD}${network.componentNetworks.size}\n"))
                    network.componentNetworks.forEach {
                        text.append(LiteralText("${Formatting.GRAY} - ${it.toString().substring(0, 8)}\n"))
                    }
                    context.player?.sendMessage(text, false)
                }
            }
            ActionResult.SUCCESS
        }else super.useOnBlock(context)
    }

}