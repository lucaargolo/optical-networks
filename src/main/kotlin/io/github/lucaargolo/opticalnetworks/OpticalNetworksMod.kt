@file:Suppress("unused")

package io.github.lucaargolo.opticalnetworks

import com.mojang.datafixers.util.Pair
import io.github.lucaargolo.opticalnetworks.blocks.*
import io.github.lucaargolo.opticalnetworks.items.blueprint.BlueprintBakedModel
import io.github.lucaargolo.opticalnetworks.items.initItems
import io.github.lucaargolo.opticalnetworks.items.initItemsClient
import io.github.lucaargolo.opticalnetworks.packets.initNetworkPackets
import io.github.lucaargolo.opticalnetworks.packets.initNetworkPacketsClient
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry
import net.fabricmc.fabric.api.client.model.ModelVariantProvider
import net.fabricmc.loader.launch.common.FabricLauncherBase
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.ModelLoader
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import java.util.function.Consumer
import java.util.function.Function

const val MOD_ID = "opticalnetworks"
val CLIENT = FabricLauncherBase.getLauncher().environmentType == EnvType.CLIENT
val CREATIVE_TAB: ItemGroup = FabricItemGroupBuilder.create(Identifier(MOD_ID, "creative_tab")).icon { ItemStack(CONTROLLER) }.build()

fun init() {
    initBlocks()
    initItems()
    initNetworkPackets()
}

fun initClient() {
    initBlocksClient()
    initItemsClient()
    initNetworkPacketsClient()
    ModelLoadingRegistry.INSTANCE.registerAppender { _: ResourceManager?, out: Consumer<ModelIdentifier?> ->
        out.accept(ModelIdentifier(Identifier(MOD_ID, "blueprint_regular"), "inventory"))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_back"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_base"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_off"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_1"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_2"), ""))
        out.accept(ModelIdentifier(Identifier(MOD_ID, "cd_3"), ""))
    }

    ModelLoadingRegistry.INSTANCE.registerVariantProvider {
        ModelVariantProvider { modelIdentifier, _ ->
            if(modelIdentifier.namespace == MOD_ID && modelIdentifier.path == "blueprint") {
                return@ModelVariantProvider object : UnbakedModel {
                    override fun getModelDependencies(): MutableCollection<Identifier> = mutableListOf()
                    override fun bake(loader: ModelLoader, textureGetter: Function<SpriteIdentifier, Sprite>, rotationScreenHandler: ModelBakeSettings, modelId: Identifier) = BlueprintBakedModel()
                    override fun getTextureDependencies(unbakedModelGetter: Function<Identifier, UnbakedModel>, unresolvedTextureReferences: MutableSet<Pair<String, String>>): MutableCollection<SpriteIdentifier> = mutableListOf()
                }
            }
            return@ModelVariantProvider null
        }
    }
}

