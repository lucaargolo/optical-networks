package io.github.lucaargolo.opticalnetworks.integration

import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.blocks.CRAFTING_TERMINAL
import io.github.lucaargolo.opticalnetworks.blocks.terminal.TerminalScreen
import io.github.lucaargolo.opticalnetworks.packets.terminalConfig
import me.shedaniel.math.Rectangle
import me.shedaniel.rei.api.DisplayHelper
import me.shedaniel.rei.api.DisplayHelper.DisplayBoundsHandler
import me.shedaniel.rei.api.EntryStack
import me.shedaniel.rei.api.RecipeHelper
import me.shedaniel.rei.api.plugins.REIPluginV0
import me.shedaniel.rei.plugin.DefaultPlugin.CRAFTING
import net.minecraft.util.Identifier

class ReiPlugin: REIPluginV0 {

    override fun getPluginIdentifier(): Identifier = Identifier(MOD_ID, "rei_plugin")

    override fun registerBounds(displayHelper: DisplayHelper) {
        displayHelper.registerHandler(object : DisplayBoundsHandler<TerminalScreen> {
            override fun getBaseSupportedClass(): Class<*>? {
                return TerminalScreen::class.java
            }

            override fun getLeftBounds(screen: TerminalScreen): Rectangle {
                return Rectangle(2, 0, (screen.width - terminalConfig.size.x)/2 - 22, screen.height)
            }

            override fun getRightBounds(screen: TerminalScreen): Rectangle {
                val startX = ((screen.width - terminalConfig.size.x)/2) + terminalConfig.size.x + 4
                return Rectangle(startX, 0, screen.width - startX - 2, screen.height)
            }

            override fun getPriority(): Float {
                return 1.0f
            }
        })
    }

    override fun registerOthers(recipeHelper: RecipeHelper) {
        recipeHelper.registerWorkingStations(CRAFTING, EntryStack.create(CRAFTING_TERMINAL));
        recipeHelper.registerAutoCraftingHandler(CustomCategoryHandler())
    }

}