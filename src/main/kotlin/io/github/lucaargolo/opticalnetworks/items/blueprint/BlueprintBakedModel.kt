package io.github.lucaargolo.opticalnetworks.items.blueprint

import io.github.lucaargolo.opticalnetworks.MOD_ID
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.render.model.json.JsonUnbakedModel
import net.minecraft.client.render.model.json.ModelOverrideList
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.ModelIdentifier
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockRenderView
import java.awt.Color
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.util.*
import java.util.function.Supplier

class BlueprintBakedModel: BakedModel, FabricBakedModel {

    override fun isVanillaAdapter(): Boolean = false

    private fun getCurrentModel(stack: ItemStack): BakedModel {
        val isShiftPressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().window.handle, 340) || InputUtil.isKeyPressed(MinecraftClient.getInstance().window.handle, 344)
        if(isShiftPressed && stack.hasTag() && stack.tag!!.contains("type") && stack.tag!!.contains("output") && (stack.tag!!.get("output") is CompoundTag || stack.tag!!.get("output") is ListTag)) {
            val type = stack.tag!!.getString("type")
            if(type == "crafting") {
                val outputStack = ItemStack.fromTag(stack.tag!!.getCompound("output") )
                return MinecraftClient.getInstance().itemRenderer.getHeldItemModel(outputStack, null, null);
            }
            if(type == "processing") {
                val outputList = stack.tag!!.get("output") as ListTag
                var outputStack = ItemStack.EMPTY
                outputList.forEach {
                    outputStack = ItemStack.fromTag(it as CompoundTag)
                    if(!outputStack.isEmpty) return@forEach
                }
                return MinecraftClient.getInstance().itemRenderer.getHeldItemModel(outputStack, null, null);
            }
        }
        val modelIdentifier = ModelIdentifier(Identifier(MOD_ID, "blueprint_regular"), "inventory")
        return MinecraftClient.getInstance().bakedModelManager.getModel(modelIdentifier)
    }

    override fun emitItemQuads(stack: ItemStack, randSupplier: Supplier<Random>, context: RenderContext) {

        val color = Color(255, 255, 255, 255).rgb
        val emitter = context.emitter

        context.pushTransform { quad ->
            quad.spriteColor(0, color, color, color, color)
            true
        }

        val model = getCurrentModel(stack)
        model.getQuads(null, null, randSupplier.get()).forEach { q ->
            emitter.fromVanilla(q.vertexData, 0, true)
            emitter.emit()
        }

        context.popTransform()
    }

    override fun emitBlockQuads(p0: BlockRenderView?, p1: BlockState?, p2: BlockPos?, p3: Supplier<Random>?, p4: RenderContext?) {}

    @Throws(IOException::class)
    private fun getReaderForResource(location: Identifier): Reader {
        val file = Identifier(location.namespace, location.path + ".json")
        val resource = MinecraftClient.getInstance().resourceManager.getResource(file)
        return BufferedReader(InputStreamReader(resource.inputStream, Charsets.UTF_8))
    }

    override fun getOverrides(): ModelOverrideList = ModelOverrideList.EMPTY

    override fun getQuads(state: BlockState?, face: Direction?, random: Random?): MutableList<BakedQuad> = mutableListOf()

    override fun getSprite() = null

    override fun hasDepth(): Boolean = false

    override fun getTransformation(): ModelTransformation? = loadTransformFromJson(Identifier("minecraft:models/item/generated"))

    override fun useAmbientOcclusion(): Boolean = true

    override fun isSideLit(): Boolean = false

    override fun isBuiltin(): Boolean = false

    private fun loadTransformFromJson(location: Identifier): ModelTransformation? {
        return try {
            JsonUnbakedModel.deserialize(getReaderForResource(location)).transformations
        } catch (exception: IOException) {
            exception.printStackTrace()
            null
        }

    }

}