package io.github.lucaargolo.opticalnetworks.integration

import io.github.lucaargolo.opticalnetworks.blocks.terminal.BlueprintTerminalScreenHandler
import io.github.lucaargolo.opticalnetworks.blocks.terminal.CraftingTerminalScreenHandler
import me.shedaniel.rei.api.RecipeHelper
import me.shedaniel.rei.plugin.DefaultPlugin
import me.shedaniel.rei.plugin.containers.CraftingContainerInfoWrapper
import me.shedaniel.rei.server.ContainerInfoHandler
import net.minecraft.screen.*
import net.minecraft.util.Identifier

class ReiContainer: Runnable {

    override fun run() {
        ContainerInfoHandler.registerContainerInfo(
            DefaultPlugin.CRAFTING,
            CraftingContainerInfoWrapper.create(CraftingTerminalScreenHandler::class.java)
        )
        ContainerInfoHandler.registerContainerInfo(
            DefaultPlugin.CRAFTING,
            CraftingContainerInfoWrapper.create(BlueprintTerminalScreenHandler.Crafting::class.java)
        )
    }

}