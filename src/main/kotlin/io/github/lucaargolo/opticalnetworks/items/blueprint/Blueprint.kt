package io.github.lucaargolo.opticalnetworks.items.blueprint

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.lucaargolo.opticalnetworks.MOD_ID
import io.github.lucaargolo.opticalnetworks.utils.areStacksCompatible
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import net.minecraft.world.World

class Blueprint(settings: Settings): Item(settings) {

    override fun hasGlint(stack: ItemStack) = stack.hasTag() && stack.tag!!.contains("type")

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        if(stack.hasTag()) {
            if(stack.tag!!.contains("type")) tooltip.add(TranslatableText("tooltip.${MOD_ID}.blueprint_type_${stack.tag!!.getString("type")}"))
            if(stack.tag!!.contains("useNbt")) {
                val nbtText = TranslatableText("tooltip.opticalnetworks.using_nbt")
                if(stack.tag!!.getBoolean("useNbt")) nbtText.append(TranslatableText("tooltip.opticalnetworks.true"))
                else nbtText.append(TranslatableText("tooltip.opticalnetworks.false"))
                tooltip.add(nbtText)
            }
            if(stack.tag!!.contains("input") && stack.tag!!.get("input") is ListTag) {
                val inputText = TranslatableText("tooltip.opticalnetworks.input")
                val listTag = stack.tag!!.get("input") as ListTag
                val mutableMap = mutableMapOf<Any, Int>()
                listTag.forEachIndexed { idx, tag ->
                    if(tag is StringTag) {
                        val inputString = tag.asString()
                        val inputJson: JsonObject? = try { JsonParser().parse(inputString).asJsonObject } catch(ignored: Exception) { null }
                        inputJson?.let { json ->
                            val mcItem: String? = try { json.get("item").asString } catch(ignored: Exception) { null }
                            val mcTag: String? = try { json.get("tag").asString } catch(ignored: Exception) { null }
                            mcItem?.let { str ->
                                val id = Identifier(str)
                                val item = Registry.ITEM.get(id)
                                val stk = ItemStack(item)
                                var found = false
                                mutableMap.keys.forEach { any ->
                                    if(any == stk.item) {
                                        found = true
                                        return@forEach
                                    }
                                }
                                if(found) mutableMap[stk.item] = mutableMap[stk.item]!! + 1
                                else mutableMap[stk.item] = 1
                            }
                            mcTag?.let { str ->
                                var found = false
                                mutableMap.keys.forEach { any ->
                                    if(any == str) {
                                        found = true
                                        return@forEach
                                    }
                                }
                                if(found) mutableMap[str] = mutableMap[str]!! + 1
                                else mutableMap[str] = 1
                            }
                        }
                    }
                    if(tag is CompoundTag) {
                        val inputStack = ItemStack.fromTag(tag)
                        if(!inputStack.isEmpty) {
                            var found = false
                            mutableMap.keys.forEach { any ->
                                if(any == inputStack.item) {
                                    found = true
                                    return@forEach
                                }
                            }
                            if(found) mutableMap[inputStack.item] = mutableMap[inputStack.item]!! + inputStack.count
                            else mutableMap[inputStack.item] = inputStack.count
                        }
                    }
                }
                mutableMap.keys.forEachIndexed { idx, any ->
                    if(any is Item) {
                        inputText.append(LiteralText(mutableMap[any].toString()+" ").formatted(Formatting.GRAY, Formatting.ITALIC))
                        inputText.append(TranslatableText(any.translationKey).formatted(Formatting.GRAY, Formatting.ITALIC))
                        if(idx < mutableMap.size-1) inputText.append(LiteralText(", ").formatted(Formatting.GRAY, Formatting.ITALIC))
                    }
                    if(any is String) {
                        inputText.append(LiteralText(mutableMap[any].toString()+" ").formatted(Formatting.GRAY, Formatting.ITALIC))
                        inputText.append(LiteralText(any).formatted(Formatting.GRAY, Formatting.ITALIC))
                        if(idx < mutableMap.size-1) inputText.append(LiteralText(", ").formatted(Formatting.GRAY, Formatting.ITALIC))
                    }
                }
                tooltip.add(inputText)
            }
            if(stack.tag!!.contains("output")) {
                val outputText = TranslatableText("tooltip.opticalnetworks.output")
                if(stack.tag!!.get("output") is CompoundTag) {
                    val outputStack = ItemStack.fromTag(stack.tag!!.getCompound("output"))
                    if(outputStack.count > 1) outputText.append(LiteralText(outputStack.count.toString()+" ").formatted(Formatting.GRAY, Formatting.ITALIC))
                    outputText.append(TranslatableText(outputStack.translationKey).formatted(Formatting.GRAY, Formatting.ITALIC))
                }
                if(stack.tag!!.get("output") is ListTag) {
                    val listTag = stack.tag!!.get("output") as ListTag
                    val stackList = mutableListOf<ItemStack>()
                    listTag.forEachIndexed { _, tag ->
                        val outputStack = ItemStack.fromTag(tag as CompoundTag)
                        if(!outputStack.isEmpty) {
                            var found = false
                            stackList.forEach { stk ->
                                if(areStacksCompatible(
                                        outputStack,
                                        stk
                                    )
                                ) {
                                    stk.increment(outputStack.count)
                                    found = true
                                }
                                return@forEach
                            }
                            if(!found) stackList.add(outputStack)
                        }
                    }
                    stackList.forEachIndexed { idx, stk ->
                        if(stk.count > 1) outputText.append(LiteralText(stk.count.toString()+" ").formatted(Formatting.GRAY, Formatting.ITALIC))
                        outputText.append(TranslatableText(stk.translationKey).formatted(Formatting.GRAY, Formatting.ITALIC))
                        if(idx < stackList.size-1) outputText.append(LiteralText(", ").formatted(Formatting.GRAY, Formatting.ITALIC))
                    }
                }
                tooltip.add(outputText)
            }
        }
    }

}